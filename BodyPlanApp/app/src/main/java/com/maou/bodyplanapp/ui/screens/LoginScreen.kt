package com.maou.bodyplanapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.maou.bodyplanapp.ui.AppViewModel

@Composable
fun LoginScreen(viewModel: AppViewModel) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var nickname by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Favorite, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text("律动健身", style = MaterialTheme.typography.headlineLarge)
        Text("让运动，成为日常。", style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("登录") }, enabled = !viewModel.busy)
            FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("注册账号") }, enabled = !viewModel.busy)
        }
        OutlinedTextField(username, { username = it }, label = { Text("账号") }, singleLine = true,
            enabled = !viewModel.busy, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("密码") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { TextButton(onClick = { visible = !visible }) { Text(if (visible) "隐藏" else "显示") } },
            enabled = !viewModel.busy, modifier = Modifier.fillMaxWidth())
        if (mode == 1) {
            OutlinedTextField(nickname, { nickname = it }, label = { Text("昵称（选填）") },
                singleLine = true, enabled = !viewModel.busy, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text("手机号（选填）") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = !viewModel.busy, modifier = Modifier.fillMaxWidth())
            Text("密码至少 6 位", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = {
            if (mode == 0) viewModel.login(username, password) else viewModel.register(username, password, nickname, phone)
        }, enabled = !viewModel.busy && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(if (viewModel.busy) "请稍候…" else if (mode == 0) "登录，开始训练" else "创建账号")
        }
        Spacer(Modifier.height(24.dp))
    }
}
