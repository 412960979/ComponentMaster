# 组件化框架  
ComponentAnnotation, ComponentApt,ComponentJavaLibs,Utils是ComponentGradlePlugin要用到的library项目  
ComponentGradlePlugin是实现组件化的gradle插件  
## 使用方法  
1.发布插件  
执行uploadArchives任务发布插件到本地maven仓库，当然发布到网络仓库也可以（5个项目都要执行）
2.使用插件  
在要使用组件化的项目顶级build里面添加maven配置，并且classpath 'com.wn.component:ComponentGradlePlugin:1.0.0'  
在app和组件化模块项目的build里面添加apply plugin: 'Component'  
具体使用可以参考组件化demo[ComponentDemo]()
 