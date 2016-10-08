package org.exoplatform.commons.cache;

import java.util.List;

import org.picocontainer.Startable;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.groovyscript.text.TemplateService;
import org.exoplatform.resolver.ApplicationResourceResolver;
import org.exoplatform.resolver.ServletResourceResolver;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class TemplateCachePopulator implements Startable {
  private static final Log    LOG = ExoLogger.getLogger(TemplateCachePopulator.class);

  TemplateService             templateService;

  ApplicationResourceResolver resolver;

  List<String>                templates;

  PortalContainer container;

  public TemplateCachePopulator(TemplateService templateService, PortalContainer container, InitParams initParams) {
    this.templateService = templateService;
    this.container = container;
    resolver = new ApplicationResourceResolver();
    resolver.addResourceResolver(new ServletResourceResolver(container.getPortalContext(), "war:"));
    resolver.addResourceResolver(new ServletResourceResolver(container.getPortalContext(), "app:"));
    resolver.addResourceResolver(new ServletResourceResolver(container.getPortalContext(), "system:"));
    resolver.addResourceResolver(new ServletResourceResolver(container.getPortalContext().getContext("/eXoResources"),
                                                             "resources:"));
    ValuesParam valuesParam = initParams.getValuesParam("templates");
    if(valuesParam != null) {
      templates = valuesParam.getValues();
    }

    init();
  }

  private void init() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        if (templates != null && !templates.isEmpty()) {
          Thread.currentThread().setContextClassLoader(container.getPortalClassLoader());
          LOG.info("Start caching templates");
          long started = System.currentTimeMillis();
          for (String templateId : templates) {
            try {
              LOG.debug("Cache template {}", templateId);
              templateService.getTemplate(templateId, resolver, true);
            } catch (Exception e) {
              LOG.debug("Error cache template {}, cause = {}", templateId, e.getMessage());
            }
          }
          long end = System.currentTimeMillis();
          long timeSpent = (end - started) / 1000;
          LOG.info("Templates cached in {} seconds", timeSpent);
        }
      }
    }).start();
  }
  
  public void start() {}

  public void stop() {}

}
