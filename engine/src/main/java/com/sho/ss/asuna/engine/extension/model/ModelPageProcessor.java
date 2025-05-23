package com.sho.ss.asuna.engine.extension.model;


import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Request;
import com.sho.ss.asuna.engine.core.Site;
import com.sho.ss.asuna.engine.core.processor.PageProcessor;
import com.sho.ss.asuna.engine.core.selector.Selector;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The extension to PageProcessor for page model extractor.
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.2.0
 */
class ModelPageProcessor implements PageProcessor
{

    private final List<PageModelExtractor> pageModelExtractorList = new ArrayList<>();

    private final Site site;

    private boolean extractLinks = true;

    public static ModelPageProcessor create(Site site, Class<?>... clazzs) {
        ModelPageProcessor modelPageProcessor = new ModelPageProcessor(site);
        for (Class<?> clazz : clazzs) {
            modelPageProcessor.addPageModel(clazz);
        }
        return modelPageProcessor;
    }


    public ModelPageProcessor addPageModel(Class<?> clazz) {
        PageModelExtractor pageModelExtractor = PageModelExtractor.create(clazz);
        pageModelExtractorList.add(pageModelExtractor);
        return this;
    }

    private ModelPageProcessor(Site site) {
        this.site = site;
    }

    @Override
    public void process(Page page) {
        for (PageModelExtractor pageModelExtractor : pageModelExtractorList) {
            if (extractLinks) {
                extractLinks(page, pageModelExtractor.getHelpUrlRegionSelector(), pageModelExtractor.getHelpUrlPatterns());
                extractLinks(page, pageModelExtractor.getTargetUrlRegionSelector(), pageModelExtractor.getTargetUrlPatterns());
            }
            Object process = pageModelExtractor.process(page);
            if (process == null || (process instanceof List && ((List<?>) process).isEmpty())) {
                continue;
            }
            postProcessPageModel(pageModelExtractor.getClazz(), process);
            page.putField(pageModelExtractor.getClazz().getCanonicalName(), process);
        }
        if (page.getResultItems().getAll().isEmpty()) {
            page.getResultItems().setSkip(true);
        }
    }

    private void extractLinks(Page page, Selector urlRegionSelector, List<Pattern> urlPatterns) {
        List<String> links;
        if (urlRegionSelector == null) {
            links = page.getHtml().links().all();
        } else {
            links = page.getHtml().selectList(urlRegionSelector).links().all();
        }
        for (String link : links) {
            for (Pattern targetUrlPattern : urlPatterns) {
                Matcher matcher = targetUrlPattern.matcher(link);
                if (matcher.find()) {
                    page.addTargetRequest(new Request(matcher.group(0)));
                }
            }
        }
    }

    protected void postProcessPageModel(Class<?> clazz, Object object) {
    }

    @Override
    public Site getSite() {
        return site;
    }

    public boolean isExtractLinks() {
        return extractLinks;
    }

    public void setExtractLinks(boolean extractLinks) {
        this.extractLinks = extractLinks;
    }
}
