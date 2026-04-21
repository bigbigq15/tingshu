package com.atguigu.tingshu.common.login;

import java.lang.annotation.*;

/**
 * 修饰controller层方法，被注解标识方法对其所在类进行增强
 * 元注解：
 *
 * @Target：指定使用位置，ElementType.TYPE：类上 ElementType.METHOD:方法上
 * @Retention：注解保留阶段
 * @Inherited：是否可以被继承
 * @Documented：通过javadoc生成类文档时候是否显示类的注解信息
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GuiGuLogin {
    /**
     * 该注解修饰方法是否必须登录，默认为必须登录
     * @return
     */
    boolean required() default true;
}
