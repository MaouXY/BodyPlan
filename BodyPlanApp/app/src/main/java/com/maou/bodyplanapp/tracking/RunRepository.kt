package com.maou.bodyplanapp.tracking

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

object RunRepository {
    private val json = Gson()
    private val mutex = Mutex()
    private val mutable = MutableStateFlow<List<LocalRun>>(emptyList())
    val sessions = mutable.asStateFlow()
    val problem = MutableStateFlow<String?>(null)
    private var owner: Long? = null

    suspend fun bind(context: Context, userId: Long) = mutex.withLock {
        if (owner == userId) return@withLock
        val loaded = withContext(Dispatchers.IO) {
            val directory = directory(context, userId)
            checkNotNull(directory.listFiles()) { "无法读取运动存储目录" }.filter { it.extension == "json" }.mapNotNull { file ->
                try {
                    val run = AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { json.fromJson(it, LocalRun::class.java) }
                    check(run.userId == userId && run.clientRecordId.matches(Regex("[a-zA-Z0-9-]{16,64}"))) { "草稿归属或编号异常" }
                    check(run.status in setOf("RUNNING", "PAUSED", "FINISHED", "UPLOADED")
                            && run.startedAt > 0 && run.durationMs >= 0 && run.distanceM.isFinite()
                            && run.distanceM >= 0 && requireNotNull(run.points).size <= 30000) { "草稿状态或统计数据异常" }
                    if (run.status == "RUNNING") {
                        Log.w("BodyPlan", "scenario=sport-recover userId=$userId clientRecordId=${run.clientRecordId} reason=process-interrupted")
                        val paused = run.copy(status = "PAUSED", segment = run.segment + 1, signal = "已恢复草稿，请继续或结束")
                        write(context, paused)
                        paused
                    } else run
                } catch (exception: Exception) {
                    Log.e("BodyPlan", "scenario=sport-recover userId=$userId file=${file.name}", exception)
                    problem.value = "部分本地运动记录读取失败，原文件已保留"
                    null
                }
            }.sortedByDescending { it.startedAt }
        }
        owner = userId
        mutable.value = loaded
    }

    suspend fun save(context: Context, run: LocalRun) = mutex.withLock {
        check(owner == run.userId) { "运动记录与当前账号不一致" }
        withContext(Dispatchers.IO) { write(context, run) }
        mutable.value = (mutable.value.filterNot { it.clientRecordId == run.clientRecordId } + run).sortedByDescending { it.startedAt }
    }

    fun tick(run: LocalRun) {
        if (owner == run.userId) mutable.value = mutable.value.map { if (it.clientRecordId == run.clientRecordId) run else it }
    }

    suspend fun discard(context: Context, clientId: String, userId: Long) = mutex.withLock {
        check(owner == userId)
        withContext(Dispatchers.IO) {
            val file = AtomicFile(File(directory(context, userId), "$clientId.json"))
            file.delete()
            check(!file.baseFile.exists()) { "本地运动记录未能删除" }
        }
        mutable.value = mutable.value.filterNot { it.clientRecordId == clientId }
        Log.i("BodyPlan", "scenario=sport-discard userId=$userId clientRecordId=$clientId")
    }

    private fun directory(context: Context, userId: Long): File =
        File(context.filesDir, "sport-runs/$userId").apply { check(isDirectory || mkdirs()) { "无法创建运动存储目录" } }

    private fun write(context: Context, run: LocalRun) {
        val file = AtomicFile(File(directory(context, run.userId), "${run.clientRecordId}.json"))
        val stream = file.startWrite()
        try {
            stream.write(json.toJson(run).toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (exception: Exception) {
            file.failWrite(stream)
            Log.e("BodyPlan", "scenario=sport-local-save userId=${run.userId} clientRecordId=${run.clientRecordId}", exception)
            throw exception
        }
    }
}
