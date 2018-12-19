package com.myclaero.claeroauto

// Permission Request Codes (House Numbers)
const val PR_CODE_CAM = 429
const val PR_CODE_LOC_F = 223
const val PR_CODE_LOC_C = 2132
const val PR_CODE_CAL_R = 323
const val PR_CODE_CAL_W = 1415
const val PR_CODE_CON_A = 700
const val PR_CODE_CON_R = 9527
const val PR_CODE_CON_W = 531

// Snackbar error types (
const val SNACK_WARNING = 1
const val SNACK_ERROR = 2

// Activity Request Codes
const val AR_CODE_ADD_VEH = 1
const val AR_CODE_ADD_VEH_CAM = 2
const val AR_CODE_VERIFY_EMAIL = 3

// VIN-Decoder error types (ZIPs)
const val DECODE_SUCCESS = 50327
const val DECODE_INVALID_VIN = 50013
const val DECODE_CONNECT_FAIL = 50014
const val DECODE_SERVER_FAIL = 50010
const val DECODE_PARSE_FAIL = 50263
const val DECODE_MATCH_FOUND = 50325

// Camera constants
const val FILE_PROVIDER_AUTH = "com.myclaero.claeroauto.fileprovider"

// API constants
const val URL_API_VIN = "https://vindecoder.p.mashape.com/v2.0/decode_vin?vin=%s"
const val URL_API_ZIP = "https://redline-redline-zipcode.p.mashape.com/rest/info.json/%s/degrees"
const val KEY_MASHAPE = "6GzrDxnPM1mshltm1rvlkRGRyF0dp1X8gdCjsnZJyu1HfOUovt"
const val KEY_STRIPE = "pk_test_YPZ8cvQ0Ff68IeE3rda9kGp4"