package com.estoque.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collection;
import java.util.List;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@Configuration
@EnableWebMvc
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@ComponentScan(basePackages = {"com.estoque.controller", "com.estoque.exception"})
// springdoc-openapi registra /v3/api-docs e /swagger-ui.html através dessas classes de configuração,
// hoje carregadas via spring-boot-autoconfigure (META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
// dentro dos jars springdoc-openapi-starter-{common,webmvc-api,webmvc-ui}). Sem o mecanismo de
// autoconfiguração do Boot rodando, nada descobre essa lista automaticamente — foi replicada aqui na
// íntegra (as três listas .imports dos três jars), incluindo as classes de @ConfigurationProperties
// (SpringDocConfigProperties, SwaggerUiConfigProperties, SwaggerUiConfigParameters,
// SwaggerUiOAuthProperties), sem as quais SwaggerConfig falha ao subir por dependência não satisfeita.
// As classes condicionadas a bibliotecas ausentes do classpath (Kotlin, Groovy, Spring HATEOAS, Spring
// Data REST) são inofensivas de importar: seus @Bean simplesmenta não registram por @ConditionalOnClass.
@Import({
        // springdoc-openapi-starter-common
        org.springdoc.core.configuration.SpringDocConfiguration.class,
        org.springdoc.core.properties.SpringDocConfigProperties.class,
        org.springdoc.core.configuration.SpringDocJavadocConfiguration.class,
        org.springdoc.core.configuration.SpringDocGroovyConfiguration.class,
        org.springdoc.core.configuration.SpringDocSecurityConfiguration.class,
        org.springdoc.core.configuration.SpringDocFunctionCatalogConfiguration.class,
        org.springdoc.core.configuration.SpringDocHateoasConfiguration.class,
        org.springdoc.core.configuration.SpringDocPageableConfiguration.class,
        org.springdoc.core.configuration.SpringDocSortConfiguration.class,
        org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration.class,
        org.springdoc.core.configuration.SpringDocDataRestConfiguration.class,
        org.springdoc.core.configuration.SpringDocKotlinConfiguration.class,
        org.springdoc.core.configuration.SpringDocKotlinxConfiguration.class,
        // SpringDocJacksonKotlinModuleConfiguration é package-private (não acessível daqui) — mas é
        // condicionada a classes Kotlin ausentes do classpath, então não haveria bean registrado mesmo
        // se pudesse ser importada.
        // springdoc-openapi-starter-webmvc-api
        org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration.class,
        org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration.class,
        // springdoc-openapi-starter-webmvc-ui
        org.springdoc.webmvc.ui.SwaggerConfig.class,
        org.springdoc.core.properties.SwaggerUiConfigProperties.class,
        org.springdoc.core.properties.SwaggerUiConfigParameters.class,
        org.springdoc.core.properties.SwaggerUiOAuthProperties.class,
        org.springdoc.core.configuration.SpringDocUIConfiguration.class
})
public class WebConfig implements WebMvcConfigurer {

    private final ApplicationContext applicationContext;

    public WebConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // extendMessageConverters (não configureMessageConverters) porque configureMessageConverters
        // SUBSTITUI a lista inteira de converters — perderíamos ByteArrayHttpMessageConverter,
        // StringHttpMessageConverter, ResourceHttpMessageConverter etc. que o Spring registra por
        // padrão. Isso quebrava o /v3/api-docs do springdoc: o handler retorna byte[], e sem
        // ByteArrayHttpMessageConverter na lista, o único converter (Jackson) "vencia" e serializava o
        // byte[] como uma string JSON em Base64 em vez de passar os bytes adiante como
        // application/json puro.
        //
        // applicationContext(...) sozinho NÃO registra os beans Module do contexto no ObjectMapper —
        // ele só configura o SpringHandlerInstantiator (usado por @JacksonInject em (de)serializers
        // customizados). Quem faz a descoberta de beans Module é o Spring Boot
        // (JacksonAutoConfiguration injeta ObjectProvider<Module> e repassa para o builder) — sem
        // Boot, precisamos replicar isso manualmente. Isso é essencial para
        // @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO) (Spring Data registra um bean
        // Module — PageModule — que converte Page em PagedModel na serialização): sem essa descoberta
        // explícita, Page é serializado no formato plano do PageImpl (sem o objeto "page" aninhado que
        // VIA_DTO produz).
        Collection<Module> jacksonModules = applicationContext.getBeansOfType(Module.class).values();
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .applicationContext(applicationContext)
                .modulesToInstall(jacksonModules.toArray(new Module[0]))
                .build();

        // Substitui só o MappingJackson2HttpMessageConverter que o Spring já registrou por padrão
        // (via addDefaultHttpMessageConverters) pelo nosso, mantendo todos os outros converters
        // padrão intactos.
        converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
        converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
    }
}
