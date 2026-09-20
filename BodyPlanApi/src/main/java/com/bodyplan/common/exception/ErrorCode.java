package com.bodyplan.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "resource not found"),
    CONFLICT(409, "business conflict"),
    TOO_MANY_REQUESTS(429, "too many requests"),
    INTERNAL_ERROR(500, "internal server error"),

    AUTH_LOGIN_FAILED(9001, "username or password is incorrect"),
    AUTH_ACCOUNT_DISABLED(9002, "account is disabled"),
    AUTH_CURRENT_USER_NOT_FOUND(9003, "current user not found"),
    AUTH_ROLE_FORBIDDEN(9004, "role is not allowed"),
    AUTH_TOKEN_INVALID_OR_EXPIRED(9005, "token is invalid or expired"),
    AUTH_USERNAME_DUPLICATED(9006, "username already exists"),

    PROFILE_NOT_FOUND(10001, "user profile not found"),
    PROFILE_ALREADY_EXISTS(10002, "user profile already exists"),
    PROFILE_INCOMPLETE(10003, "user profile is incomplete"),

    BODY_DATA_NOT_FOUND(10101, "body data not found"),
    BODY_DATA_INVALID(10102, "body data invalid"),

    FITNESS_TEST_NOT_FOUND(10201, "fitness test not found"),
    FITNESS_TEST_REQUIRED(10202, "fitness test is required before planning"),

    EXERCISE_NOT_FOUND(10301, "exercise not found"),
    EXERCISE_DISABLED(10302, "exercise is disabled"),
    EXERCISE_TAG_NOT_FOUND(10303, "exercise tag not found"),

    PLAN_NOT_FOUND(10401, "training plan not found"),
    PLAN_STATUS_INVALID(10402, "training plan status invalid"),
    PLAN_GENERATE_FAILED(10403, "training plan generate failed"),
    PLAN_ITEM_NOT_FOUND(10404, "training plan item not found"),
    PLAN_ALREADY_ACTIVE(10405, "active training plan already exists"),

    CHECKIN_NOT_FOUND(10501, "checkin record not found"),
    CHECKIN_ALREADY_EXISTS(10502, "checkin record already exists"),
    CHECKIN_NOT_BELONG_TO_USER(10503, "checkin record does not belong to current user"),
    CHECKIN_DATE_IN_FUTURE(10504, "checkin date is in the future"),

    RUNNING_RECORD_NOT_FOUND(10601, "running record not found"),
    RUNNING_RECORD_INVALID(10602, "running record invalid"),

    FILE_TYPE_INVALID(10701, "file type invalid"),
    FILE_SIZE_EXCEEDED(10702, "file size exceeded");

    private final int code;
    private final String message;
}
