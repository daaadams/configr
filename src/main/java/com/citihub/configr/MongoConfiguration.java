package com.citihub.configr;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoClientFactoryBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.index.TextIndexDefinition.TextIndexDefinitionBuilder;
import com.citihub.configr.mongostorage.MongoNamespaceDeserializer;
import com.citihub.configr.mongostorage.MongoNamespaceSerializer;
import com.citihub.configr.namespace.Namespace;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Strings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoCredential;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class MongoConfiguration {

  @Value("${mongodb.db}")
  private String db;

  @Value("${mongodb.uri}")
  private String uri;

  @Value("${mongodb.username}")
  private String username;

  @Value("${mongodb.password}")
  private String password;

  @Value("${mongodb.auth_db}")
  private String authDB;
  
  @Autowired
  private ApplicationContext appContext;
  
  @Profile("!test")
  public @Bean MongoClientFactoryBean mongo() {
       MongoClientFactoryBean mongo = new MongoClientFactoryBean();
       mongo.setConnectionString(new ConnectionString(uri));
       if(!Strings.isNullOrEmpty(username))
         mongo.setCredential( new MongoCredential[] {
           MongoCredential.createCredential(username, db, password.toCharArray()) } );
       return mongo;
  }
  
  public @Bean({"objectMapper"}) ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.findAndRegisterModules();

    SimpleModule module = new SimpleModule();
    module.addSerializer(Namespace.class, new MongoNamespaceSerializer());
    module.addDeserializer(Namespace.class, new MongoNamespaceDeserializer());
    mapper.registerModule(module);

    return mapper;
  }
  
  @PostConstruct
  public void postConstruct() {
    log.info("Configuration all done! Ensuring indexes are set.");
    MongoTemplate mongoTemplate = appContext.getBean(MongoTemplate.class);
    try {
      ensureIndexes(mongoTemplate);
    } catch (NullPointerException e) {
      log.error("DB is null - expected during test runs but this is a "
          + "horrible hack.");
    }
  }
 
  private void ensureIndexes(MongoTemplate mongoTemplate) {
    TextIndexDefinition textIndex = new TextIndexDefinitionBuilder()
        .onField("_id")
        .build();

    mongoTemplate.indexOps(Namespace.class).ensureIndex(textIndex);
  }

  
}