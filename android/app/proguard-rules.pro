# 默认无需额外规则；混淆开启时保留数据模型与 Retrofit 接口
-keepattributes Signature
-keep class com.xiaolanshu.app.data.Models.** { *; }
-keep interface com.xiaolanshu.app.data.ApiService { *; }
