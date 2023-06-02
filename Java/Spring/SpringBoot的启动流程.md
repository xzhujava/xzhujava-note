# SpringBoot启动流程

>Spring Boot是由Pivotal团队提供的基于Spring的框架，该框架使用了特定的方式来进行配置，从而使开发人员不再需要定义样板化的配置。Spring Boot集成了绝大部分目前流行的开发框架，就像Maven集成了所有的JAR包一样，Spring Boot集成了几乎所有的框架，使得开发者能快速搭建Spring项目。

SpringBoot相较于Spring有如下几个优势：

* 可快速构建独立的 Spring 应用
* 直接嵌入Tomcat、Jetty 和Undertow 服务器(无须部署WAR文件)
* 通过依赖启动器简化构建配置
* 自动化配置Spring和第三方库
* 提供生产就绪功能
* 极少的代码生成和XML配置

虽然说 Spring Boot有诸多的优点，但Spring Boot也有一些缺点。例如，Spring Boot入门较为简单，但是深入理解和学习却有一定的难度。那么这篇文章就来简述SpringBoot的启动流程。

首先需要一个@SpringBootApplication注解的启动类：

```java

@SpringBootApplication
public class SpringBootTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootTestApplication.class, args);
    }
}
```

@SpringBootApplication本质上由@SpringBootConfiguration、@EnableAutoConfiguration和@ComponentScan构成

```java
...

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        ...
)
public @interface SpringBootApplication {
    ...
}
```

@EnableAutoConfiguration是最为核心的，有了它之后在启动时就会导入“自动配置”AutoConfigurationImportSelector类，这个类会将所有符合条件的@Configuration配置都进行加载

@SpringBootConfiguration等同于@Configuration，就是将这个类标记为配置类会被加载到容器中

@ComponentScan就是自动扫描并加载符合条件的Bean

其实如果配置类中不需要增加配置内容也不需要 指定扫描路径那可以使用@EnableAutoConfiguration替代@SpringBootApplication也可以完成启动

注解完成后，运行的起点就是SpringApplication#run()这个方法

```java
public static ConfigurableApplicationContext run(Class<?>[]primarySources,String[]args){
    return new SpringApplication(primarySources).run(args);
}
```

在run开始执行后会经历四个阶段：服务构建、环境准备、容器创建、填充容器。

##### 服务构建

服务构建所说的服务一个功能非常强大的Spring服务对象SpringApplication 这个阶段就是用一大堆零件把服务组装出来，下面是SpringApplication的构造方法：

```java
public SpringApplication(ResourceLoader resourceLoader,Class<?>...primarySources){
    this.resourceLoader=resourceLoader;
    Assert.notNull(primarySources,"PrimarySources must not be null");
    this.primarySources=new LinkedHashSet<>(Arrays.asList(primarySources));
    this.webApplicationType=WebApplicationType.deduceFromClasspath();
    this.bootstrapRegistryInitializers=new ArrayList<>(
            getSpringFactoriesInstances(BootstrapRegistryInitializer.class));
    setInitializers((Collection)getSpringFactoriesInstances(ApplicationContextInitializer.class));
    setListeners((Collection)getSpringFactoriesInstances(ApplicationListener.class));
    this.mainApplicationClass=deduceMainApplicationClass();
}
```

在构造方法中有两个参数：resourceLoader和primarySources分别是资源加载器、主方法类，首先要把传入的这两个参数记录在内存中，然后逐一判断对应的服务类是否存在，来确认服务类型

默认是SERVLET，即基于Servlet的web服务如tomcat，还有响应式非阻塞服务REACTIVE，如Spring-webflux，还有什么都不是用的NONE。

确定了选择哪个web服务之后然后就要加载初始化类了，接下来会读取所有META-INF/spring.factories文件中的注册初始化、上下文初始化和监听器这三类配置
即BootstrapRegistryInitializer、ApplicationContextInitializer和ApplicationListener。

spring没有默认的注册初始化配置，而spring-boot和SpringbootAutoConfigure这两个工程中配置了7个上下文初始化和8个监听器这些配置信息会在后续的启动过程中使用到，我们也可以自定义这三个配置，只需要将其放到工程中的spring.factories文件中，springboot就会将他们一并加载。

接下来会通过“运行栈”stackTrace判断出main方法所在的类，大概率就是启动类本身，后续过程会使用到。

这样spring服务SpringApplication就构造完成了。然后就是调用run方法进入“环境准备”阶段

##### 环境准备

这个阶段的目的是要给即将诞生的容器做充分的养分准备，下面是run方法的源码：

```java
public ConfigurableApplicationContext run(String... args) {
    long startTime = System.nanoTime();
    //创建 DefaultBootstrapContext，用于在 Spring 应用程序上下文之前收集信息和配置
    DefaultBootstrapContext bootstrapContext = createBootstrapContext();
    ConfigurableApplicationContext context = null;
    //配置 Headless 属性，以确保在没有头部的环境中运行 Spring 应用程序时不会出现问题
    configureHeadlessProperty();
    //获取 SpringApplicationRunListeners 实例，这个实例包含了所有注册的监听器
    SpringApplicationRunListeners listeners = getRunListeners(args);
    //向监听器发送启动事件
    listeners.starting(bootstrapContext, this.mainApplicationClass);
    try {
        //解析应用程序参数，准备环境
        ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
        ConfigurableEnvironment environment = prepareEnvironment(listeners, bootstrapContext, applicationArguments);
        //打印 Banner
        Banner printedBanner = printBanner(environment);
        //创建 Spring 应用程序上下文
        context = createApplicationContext();
        context.setApplicationStartup(this.applicationStartup);
        //准备 Spring 应用程序上下文，包括设置环境、添加监听器、打印 Banner
        prepareContext(bootstrapContext, context, environment, listeners, applicationArguments, printedBanner);
        //刷新 Spring 应用程序上下文
        refreshContext(context);
        //调用所有注册的 Runner
        afterRefresh(context, applicationArguments);
        Duration timeTakenToStartup = Duration.ofNanos(System.nanoTime() - startTime);
        //如果启用了日志启动信息，则记录启动时间
        if (this.logStartupInfo) {
            new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), timeTakenToStartup);
        }
        //向监听器发送已启动事件
        listeners.started(context, timeTakenToStartup);
        //检查应用程序是否已运行，并向监听器发送已准备就绪事件
        callRunners(context, applicationArguments);
    }catch (Throwable ex) {
        //异常处理
        ...
    }
        ...
    //返回 Spring 应用程序上下文
    return context;
}
```

根据上面代码可以看到，首先会通过createBootstrapContext()方法 new一个后续会陆续使用到的启动上下文BootstrapContext

```java
private DefaultBootstrapContext createBootstrapContext() {
    DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();
    this.bootstrapRegistryInitializers.forEach((initializer) -> initializer.initialize(bootstrapContext));
    return bootstrapContext;
}
```

在createBootstrapContext()方法中可以看到创建bootstrapContext的同时逐一调用刚刚加载的“启动注册初始化器”bootstrapRegistryInitializer中的初始化initialize方法，不过刚才也提到了spring并没有默认的BootstrapRegistryInitializer，所以默认并不执行什么

接下来在configureHeadlessProperty()方法中将java.awt.headless这个设置改为true，表示缺少显示器、键盘等输入设备也可以正常启动：

```java
private void configureHeadlessProperty() {
    System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS,
        System.getProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(this.headless)));
}
```

然后会启动运行监听器SpringApplicationRunListeners同时发布启动事件，getRunListeners()方法代码如下：

```java
private SpringApplicationRunListeners getRunListeners(String[] args) {
    //创建一个ArgumentResolver对象，用于解析应用程序和命令行参数
    ArgumentResolver argumentResolver = ArgumentResolver.of(SpringApplication.class, this);
    //将参数数组args与ArgumentResolver对象合并
    argumentResolver = argumentResolver.and(String[].class, args);
    //获取所有SpringApplicationRunListener实例，并将其与参数解析器argumentResolver一起传递
    List<SpringApplicationRunListener> listeners = getSpringFactoriesInstances(SpringApplicationRunListener.class,
        argumentResolver);
    //如果存在SpringApplicationHook对象，使用该对象获取SpringApplicationRunListener，并将其添加到列表中
    SpringApplicationHook hook = applicationHook.get();
    SpringApplicationRunListener hookListener = (hook != null) ? hook.getRunListener(this) : null;
    if (hookListener != null) {
        listeners = new ArrayList<>(listeners);
        listeners.add(hookListener);
    }
    //返回一个SpringApplicationRunListeners对象，该对象包含日志记录器、监听器列表和应用程序启动对象
    return new SpringApplicationRunListeners(logger, listeners, this.applicationStartup);
}
```

它获取并加载springboot工程spring.factories配置文件中的EventPublishingRunListener，它在启动时也会将刚刚所说的8个ApplicationListener都进行引入这样就可以通过监听这些事件然后在启动流程中加入自定义逻辑了

接下来就要通过prepareEnvironment方法组装启动参数了：

```java
private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners,
	    DefaultBootstrapContext bootstrapContext, ApplicationArguments applicationArguments) {
    // 创建并配置环境
    ConfigurableEnvironment environment = getOrCreateEnvironment();
    configureEnvironment(environment, applicationArguments.getSourceArgs());
    ConfigurationPropertySources.attach(environment);
    listeners.environmentPrepared(bootstrapContext, environment);
    DefaultPropertiesPropertySource.moveToEnd(environment);
    Assert.state(!environment.containsProperty("spring.main.environment-prefix"),
        "Environment prefix cannot be set via properties.");
    bindToSpringApplication(environment);
    if (!this.isCustomEnvironment) {
        EnvironmentConverter environmentConverter = new EnvironmentConverter(getClassLoader());
        environment = environmentConverter.convertEnvironmentIfNecessary(environment, deduceEnvironmentClass());
    }
    ConfigurationPropertySources.attach(environment);
    return environment;
}
```

首先第一步就是构造一个可配置环境ConfigurableEnvironment，根据不同的web服务类型会构造不同的环境，同样默认是Servlet，构造之后会加载很多诸如系统环境变量SystemEnvironment、jvm系统属性 systemProperties等在内的四组配置信息，
然后把这些配置信息都加载到一个叫做propertySources的内存集合中，这样后续使用到这些信息就无须重新加载了

```java
protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
    if (this.addConversionService) {
        environment.setConversionService(new ApplicationConversionService());
    }
    configurePropertySources(environment, args);
    configureProfiles(environment, args);
}
```

可以看到这时也会通过配置环境configureEnvironment方法将我们启动时传入的环境参数args进行设置，例如启动时传入的诸如“开发/生产”环境配置等都会在这一步进行加载，
同时在propertySources集合的首个位置添加一个值为空的配置内容“configurationProperties”后续会被使用。
接下来就会发布环境准备完成这个事件，刚加载进来的8个Listener会监听到这个事件，其中的部分监听器会进行相应处理，诸如环境配置后处理监听器EnvironmentPostProcessorApplicationListener会去加载spring.factories配置文件中“环境配置后处理器”EnvironmentPostProcessor

这里要注意监听器通过观察者模式设计是逐一串行执行并不是异步并行，需要等待所有监听器都处理完成之后才会走后续的逻辑。
环境绑定之后剩下的就是考虑到刚创建的可配置环境在一系列过程中可能会有变化进而做的补偿，通过二次更新保证匹配紧接着将spring.beaninfo.ignore设为true，表示不加载Bean的元数据信息，同时打印Banner图，这样环境准备阶段就完成了

##### 容器创建

在这一阶段会将上一阶段准备好的各种养分进行组合孵化最核心的“容器”，所谓容器就是内部有很多奇奇怪怪属性，集合以及配套功能的结构体ApplicationContext，也就是“应用程序上下文”，当然叫做“容器”更好理解

通过createApplicationContext方法来创建容器

```java
protected ConfigurableApplicationContext createApplicationContext() {
    return this.applicationContextFactory.create(this.webApplicationType);
}

//下面是ApplicationContextFactory中create方法的默认实现
@Override
public ConfigurableApplicationContext create(WebApplicationType webApplicationType) {	    
    try {
        return getFromSpringFactories(webApplicationType, ApplicationContextFactory::create,
        this::createDefaultApplicationContext);
    }catch (Exception ex) {
        throw new IllegalStateException("Unable create a default ApplicationContext instance, "
        + "you may need a custom ApplicationContextFactory", ex);
    }
}

private ConfigurableApplicationContext createDefaultApplicationContext() {
    if (!AotDetector.useGeneratedArtifacts()) {
        return new AnnotationConfigApplicationContext();
    }
    return new GenericApplicationContext();
}
```

创建过程很简单，首先根据服务类型创建容器ConfigurableApplicationContext，默认的服务类型是SERVLET，所以创建的是“注解配置的Servlet-Web服务容器”，即AnnotationConfigServletWebServerApplicationContext

在这个过程中，会构造诸如存放和生产我们bean实例的Bean工厂，DefaultListableBeanFactory用来解析@Component、@ComponentScan等注解的配置类后处理器ConfigurationClassPostProcessor，用来解析@Autowired、@Value、@Inject等注解的“自动注解Bean后处理器”AutowiredAnnotationBeanPostProcessor等在内的属性对象，把它们都放入容器中后就要通过prepareContext方法对容器中的部分属性进行初始化了，先用postProcessApplicationContext方法设置Bean名称生成器、资源加载器、类型转换器等；

接着就要执行之前加载进来的上下文初始化ApplicationContextInitializer了，具体代码实现在prepareContext()方法中可以看到：

```java
private void prepareContext(DefaultBootstrapContext bootstrapContext, ConfigurableApplicationContext context,
        ConfigurableEnvironment environment, SpringApplicationRunListeners listeners,
        ApplicationArguments applicationArguments, Banner printedBanner) {
    // 将环境设置到应用程序上下文中
    context.setEnvironment(environment);
    //对应用程序上下文进行后处理
    postProcessApplicationContext(context);
    //如果需要，添加AOT生成的初始化器
    addAotGeneratedInitializerIfNecessary(this.initializers);
    //应用所有初始化器
    applyInitializers(context);
    //通知所有SpringApplicationRunListeners上下文已经准备好了
    listeners.contextPrepared(context);
    //关闭DefaultBootstrapContext
    bootstrapContext.close(context);
    //如果需要，记录启动信息和启动配置文件信息
    if (this.logStartupInfo) {
        logStartupInfo(context.getParent() == null);
        logStartupProfileInfo(context);
    }
    // Add boot specific singleton beans.向应用程序上下文中添加Spring Boot特定的单例bean
    ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
    beanFactory.registerSingleton("springApplicationArguments", applicationArguments);
    if (printedBanner != null) {
        beanFactory.registerSingleton("springBootBanner", printedBanner);
    }
    //如果需要，设置允许循环依赖和允许覆盖bean定义
    if (beanFactory instanceof AbstractAutowireCapableBeanFactory autowireCapableBeanFactory) {
        autowireCapableBeanFactory.setAllowCircularReferences(this.allowCircularReferences);
        if (beanFactory instanceof DefaultListableBeanFactory listableBeanFactory) {
            listableBeanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
        }
    }
    //如果需要延迟初始化，向应用程序上下文中添加LazyInitializationBeanFactoryPostProcessor
    if (this.lazyInitialization) {
        context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
    }
    //向应用程序上下文中添加PropertySourceOrderingBeanFactoryPostProcessor
    context.addBeanFactoryPostProcessor(new PropertySourceOrderingBeanFactoryPostProcessor(context));
    //如果不是使用AOT生成的构件，则加载所有源
    if (!AotDetector.useGeneratedArtifacts()) {
        // Load the sources
        Set<Object> sources = getAllSources();
        Assert.notEmpty(sources, "Sources must not be empty");
        load(context, sources.toArray(new Object[0]));
    }
    //通知所有SpringApplicationRunListeners上下文已经加载完毕
    listeners.contextLoaded(context);
}
```

默认加载了七个容器id、警告日志处理、日志监听都是在这里实现的；在发布“容器准备完成”监听事件之后会陆续为容器注册“启动参数”、Banner、“Bean引用策略”和懒加载策略等等。

之后通过Bean定义加载器将启动类在内的资源加载到Bean定义池BeanDefinitionMap中以便后续根据Bean定义创建Bean对象然后发布一个资源加载完成事件，这样最核心的“容器”就创建完成啦

##### 填充容器

下面就是最后的“填充容器”，在这一步中会生产springBoot自身提供的和我们自定义的所有Bean对象，并且放在刚刚创建好的容器中这个过程就是自动装配。

这个过程大体分为十二个小步骤，这些小步骤中不但包含了之前介绍过的Bean生命周期管理同时还会构造和启动一个web服务器，这样我们就可以通过web方式来使用了
这十二个步骤主要是我们经常说的IOC控制反转的具体细节，这里就不展开讲了，在这十二个步骤完成之后，发布启动完成事件的同时会回调我们自定义实现的Runner接口来处理一些执行后定制化需求。

以上就是SpringBoot完整的启动流程