Spring生态
---
![Spring](image/spring.png)
## Spring
#### 1使用Spring框架的好处
* 控制反转：Spring通过控制反转实现了松散耦合，对象们给出它们的依赖，而不是创建或查找依赖的对象们
* 面向切面的编程(AOP)：Spring支持面向切面的编程，并且把应用业务逻辑和系统服务分开 容器：Spring 包含并管理应用中对象的生命周期和配置
* MVC框架：Spring的WEB框架是个精心设计的框架，是Web框架的一个很好的替代品
* 事务管理：Spring 提供一个持续的事务管理接口，可以扩展到上至本地事务下至全局事务（JTA）
* 异常处理：Spring 提供方便的API把具体技术相关的异常（比如由JDBC，Hibernate or JDO抛出的）转化为一致的unchecked 异常
* 非侵入式设计：Spring是一种非侵入式(non-invasive)框架，它可以使应用程序代码对框架的依赖最小化
#### 2什么是Spring容器

#### spring中bean的生命周期

1. 实例化bean(Bean工厂new bean)
2. 属性赋值
3. 初始化bean(生成代理对象在初始化时完成)

#### spring的三级缓存

##### 为什么要三级缓存，二级不可以？是为了解决什么问题？

1. AOP代理对象
2. Spring为了解决因循环依赖破坏生命周期流程 **代码规范**

#### AOP(面向切面编程)
主要是用动态代理技术-jdk动态代理、cglib动态代理，Spring生命周期-初始化时创建代理对象，主要是用代理模式、责任链模式(通知-Around、After、Before等)顺序执行，类比filter

#### Spring Bean的线程安全问题
##### Spring Bean的作用域
单例bean(singleton)、多例bean(prototype)、request、session

###### scope:prototype模式下
成员变量->线程安全(因为每次请求都是一个新的bean)
静态变量->线程不安全(随类创建加载，由这个类创建出来的实例对象共享这个静态变量)

###### scope:singleton模式下
成员变量->线程不安全
静态变量->线程不安全

###### 单例模式下如何保证线程安全？
**使用ThreadLocal**

#### SpringBoot的启动流程

1. 初始化SpringApplication对象
    * 推断应用类型，根据不同的应用类型，完成一些容器的初始化工作
    * 实例化META-INF/spring.factories中已配置的ApplicationContextInitializer初始化器类
    * 实例化META-INF/spring.factories中已配置的ApplicationListener监听器类
2. 执行run方法
    * 启动监听器
    * 构建一个应用上下文环境对象ConfigurableEnvironment，将一些环境信息，如系统信息、jdk信息、maven信息，
    配置文件中的配置，全部加在进ConfigurableEnvironment中
    * 初始化应用上下文对象，创建DefaultListableBeanFactory对象
    * prepareContext
        * 设置容器初始化环境信息
        * 执行容器的后置处理器
        * 将启动类的BeanDefinition加载到容器中
        * 监听器发布事件
    * Spring的refresh()

#### SpringBoot的自动配置(SPI机制-最大优势解耦)

