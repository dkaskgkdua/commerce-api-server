package com.commerce.song.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.hibernate5.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = {"com.commerce.song.domain.mapper"}, sqlSessionFactoryRef = "sqlSessionFactory")
public class DatabaseConfig {
    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource, ApplicationContext applicationContext) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setMapperLocations(applicationContext.getResources("classpath:/mapper/*.xml"));
        sqlSessionFactoryBean.setConfigLocation(applicationContext.getResource("classpath:/config/mapper-config.xml"));
        sqlSessionFactoryBean.setTypeAliasesPackage("com.commerce.song.domain");

        return sqlSessionFactoryBean.getObject();
    }

    @Bean(name = "sqlSessionTemplate")
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
    // ???????????? ?????? ???????????? ???????????? ?????? EntityManager??? ?????? EntityManagerFactory??? ???????????????.
    // ???????????? ???????????? ????????? ?????? ????????? ????????? ????????? ?????? ?????? ?????? ??????
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        // ddl ?????? ??????
        vendorAdapter.setGenerateDdl(true);
        // sql show ??????
        vendorAdapter.setShowSql(true);

        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(dataSource);
        // entity scan ??? ?????????.
        entityManagerFactory.setPackagesToScan("com.commerce.song.domain.entity");
        entityManagerFactory.setJpaVendorAdapter(vendorAdapter);
        entityManagerFactory.afterPropertiesSet();
        entityManagerFactory.setJpaProperties(jpaProperties());

        return entityManagerFactory;
    }

    // transactional ??????
//    @Bean
//    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource ) throws Exception {
//        // MyBatis transactional
//        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
//        dataSourceTransactionManager.setDataSource(dataSource);
//
//        // JPA transactional
//        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
//        jpaTransactionManager.setEntityManagerFactory(entityManagerFactory(dataSource).getObject());
//
//        // Chained transaction manager (MyBatis X JPA)
//        ChainedTransactionManager transactionManager = new ChainedTransactionManager(jpaTransactionManager, dataSourceTransactionManager);
//        return transactionManager;
//    }
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) throws Exception {
        // JPA transactional
        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
        jpaTransactionManager.setEntityManagerFactory(entityManagerFactory);

        return jpaTransactionManager;
    }
    @Bean
    public HibernateExceptionTranslator hibernateExceptionTranslator() {
        return new HibernateExceptionTranslator();
    }

    private Properties jpaProperties() {
        Properties properties = new Properties();

        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
        properties.put("hibernate.format_sql", true);

        return properties;
    }
}
