# sealtalk-eclipse
###因为融云的sealtalk源码是Android studio 所以我把他转成了eclipse，在改的时候，因为版本的原因发现了一些问题，我在这里总结一下，对于集成融云callkit的eclipse同学会有帮助。
融云callkitsdk在eclipse中的错误
-----------------------------------  
###1.把java代码拷贝到src文件目录下。
###2.如果遇到这个错误Call requires API level 4 (current min is 1): android.content.Intent#setPack ，在工程上点击右键 -> Android Tools -> Clear Lint Markers，即可。
###3.Build.VERSION_CODES.M M cannot be resolved or is not a field 这个错误还是版本的原因，将Build.VERSION_CODES.M改成23.
###4.在MultiAudioCallActivity里面 onRequestPermissionsResult  会报错，需要在BaseCallActivity  实现ActivityCompat.OnRequestPermissionsResultCallback 接口类即可。
###5.要用23 版本以上的V4包。在23以下版本的V4包中并没有这几个方法。
    ActivityCompat.checkSelfPermission()
    ActivityCompat.requestPermissions()
    ActivityCompat.OnRequestPermissionsResultCallback
    ActivityCompat.shouldShowRequestPermissionRationale()
###6.以上就是一些错误的解决方式，如果还有问题，希望大家能够联系我，共同学习。
