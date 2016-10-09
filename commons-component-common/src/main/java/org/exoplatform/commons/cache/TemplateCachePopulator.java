package org.exoplatform.commons.cache;

import java.security.PrivilegedAction;
import java.util.List;

import org.picocontainer.Startable;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.groovyscript.text.TemplateService;
import org.exoplatform.resolver.ApplicationResourceResolver;
import org.exoplatform.resolver.ResourceKey;
import org.exoplatform.resolver.ServletResourceResolver;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class TemplateCachePopulator implements Startable {
  private static final Log    LOG = ExoLogger.getLogger(TemplateCachePopulator.class);

  TemplateService             templateService;


  List<String>                templates;

  PortalContainer             container;

  public TemplateCachePopulator(TemplateService templateService, PortalContainer container, InitParams initParams) {
    this.templateService = templateService;
    this.container = container;

    ValuesParam valuesParam = initParams.getValuesParam("templates");
    if (valuesParam != null) {
      templates = valuesParam.getValues();
    }

    init();
  }

  private void init() {
    if (templates != null && !templates.isEmpty()) {
      int middleOfTemplates = templates.size() / 2;
      final List<String> firstPartOfTemplates = templates.subList(0, middleOfTemplates);
      final List<String> secondPartOfTemplates = templates.subList(middleOfTemplates, templates.size());

      executeCacheTemplates(firstPartOfTemplates, 1);
      executeCacheTemplates(secondPartOfTemplates, 2);
    }
  }

  private void executeCacheTemplates(final List<String> partOfTemplates, int i) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        SecurityHelper.doPrivilegedAction(new PrivilegedAction<ClassLoader>() {
          public ClassLoader run() {
            Thread.currentThread().setContextClassLoader(container.getPortalClassLoader());
            return Thread.currentThread().getContextClassLoader();
          }
        });

        CachePopulatorResourceResolver resolver = new CachePopulatorResourceResolver();

        resolver.addResourceResolver(new ServletResourceResolver(container.getPortalContext(), "war:"));
        resolver.addResourceResolver(new ServletResourceResolver(container.getPortalContext(), "app:"));
        resolver.addResourceResolver(new ServletResourceResolver(container.getPortalContext(), "system:"));
        resolver.addResourceResolver(new ServletResourceResolver(container.getPortalContext().getContext("/eXoResources"),
                                                                 "resources:"));
        LOG.info("Start caching templates");
        long started = System.currentTimeMillis();
        for (String templateId : partOfTemplates) {
          try {
            LOG.debug("Cache template {}", templateId);
            if(templateId.contains("@")) {
              String[] templateParts = templateId.split("@");
              templateId = templateParts[1];
              resolver.setContextPath(templateParts[0]);
            } else {
              resolver.setContextPath(null);
            }
            templateService.getTemplate(templateId, resolver, true);
          } catch (Exception e) {
            LOG.debug("Error cache template {}, cause = {}", templateId, e.getMessage());
          }
        }
        long end = System.currentTimeMillis();
        long timeSpent = (end - started) / 1000;
        LOG.info("Templates cached in {} seconds", timeSpent);
      }
    }, "TemplateCachePopulator - part " + i).start();
  }

  public void start() {
  }

  public void stop() {
  }

  public static class CachePopulatorResourceResolver extends ApplicationResourceResolver {

    private String contextPath;

    public CachePopulatorResourceResolver() {
    }

    @Override
    public ResourceKey createResourceKey(String url) {
      
      return new ResourceKey(contextPath.hashCode(), url);
    }
    
    public void setContextPath(String contextPath) {
      this.contextPath = contextPath;
    }
  }
}
