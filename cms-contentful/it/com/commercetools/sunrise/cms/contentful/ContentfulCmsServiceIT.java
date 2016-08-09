package com.commercetools.sunrise.cms.contentful;

import com.commercetools.sunrise.cms.CmsPage;
import com.commercetools.sunrise.cms.CmsServiceException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

public class ContentfulCmsServiceIT {
    private static final List<Locale> SUPPORTED_LOCALES = Collections.singletonList(Locale.GERMANY);

    // credentials for contentful demo account
    private static final String IT_PREFIX = "CONTENTFUL_";
    private static final String IT_CF_SPACE_ID = IT_PREFIX + "SPACE_ID";
    private static final String IT_CF_TOKEN = IT_PREFIX + "TOKEN";
    private static final String PAGE_TYPE_NAME = "page";
    private static final String PAGE_TYPE_ID_FIELD_NAME = "slug";
    private ContentfulCmsService contentfulCmsService;

    private static String spaceId() {
        return getValueForEnvVar(IT_CF_SPACE_ID);
    }

    private static String token() {
        return getValueForEnvVar(IT_CF_TOKEN);
    }

    @Before
    public void setUp() throws Exception {
        contentfulCmsService = ContentfulCmsService.of(spaceId(), token(), PAGE_TYPE_NAME, PAGE_TYPE_ID_FIELD_NAME);
    }

    @Test
    public void whenCouldNotFetchEntry_thenThrowException() throws Exception {
        final ContentfulCmsService cmsService = ContentfulCmsService.of("", "", PAGE_TYPE_NAME, PAGE_TYPE_ID_FIELD_NAME);

        Throwable thrown = catchThrowable(() -> waitAndGet(cmsService.page("home", SUPPORTED_LOCALES)));

        assertThat(thrown).isInstanceOf(ExecutionException.class).hasMessageContaining("Could not fetch content for home");
        assertThat(thrown).hasCauseInstanceOf(CmsServiceException.class);
    }

    @Test
    public void whenAskForExistingStringContentThenGet() throws Exception {
        Optional<CmsPage> content = waitAndGet(contentfulCmsService.page("finn", SUPPORTED_LOCALES));
        assertThat(content).isPresent();

        assertThat(content.get().field("pageContent.description")).contains("Fearless Abenteurer! Verteidiger von Pfannkuchen.");
    }

    @Test
    public void whenAskForExistingStringContentAndLocalesAreEmptyThenGetDefaultLocaleContent() throws Exception {
        Optional<CmsPage> content = waitAndGet(contentfulCmsService.page("finn", Collections.emptyList()));
        assertThat(content).isPresent();

        assertThat(content.get().field("pageContent.description")).contains("Fearless Abenteurer! Verteidiger von Pfannkuchen.");
    }

    @Test
    public void whenAskForExistingStringContentAndLocalesAreNullThenGetDefaultLocaleContent() throws Exception {
        Optional<CmsPage> content = waitAndGet(contentfulCmsService.page("finn", null));
        assertThat(content).isPresent();

        assertThat(content.get().field("pageContent.description")).contains("Fearless Abenteurer! Verteidiger von Pfannkuchen.");
    }

    @Test
    public void whenAskForExistingStringContentWithNotDefaultLocaleThenGet() throws Exception {
        Optional<CmsPage> content = waitAndGet(contentfulCmsService.page("finn", Collections.singletonList(Locale.ENGLISH)));
        assertThat(content).isPresent();

        assertThat(content.get().field("pageContent.description")).contains("Fearless adventurer! Defender of pancakes.");
    }

    @Test
    public void whenAskForExistingStringContentWithNotDefinedLocaleThenNotPresent() throws Exception {
        Optional<CmsPage> content = waitAndGet(contentfulCmsService.page("finn", Collections.singletonList(Locale.ITALIAN)));
        assertThat(content).isEmpty();
    }

    @Test
    public void whenAskForNotExistingStringContentThenNotPresent() throws Exception {
        Optional<CmsPage> content = waitAndGet(contentfulCmsService.page("finn", SUPPORTED_LOCALES));
        assertThat(content).isPresent();
        assertThat(content.get().field("pageContent.notExistingField")).isEmpty();
    }

    @Test
    public void whenAskForExistingAssetContentThenGet() throws Exception {
        Optional<CmsPage> content = waitAndGet(contentfulCmsService.page("jacke", SUPPORTED_LOCALES));
        assertThat(content).isPresent();
        assertThat(content.get().fieldOrEmpty("pageContent.image")).isEqualToIgnoringCase("//images.contentful.com/l6chdlzlf8jn/2iVeCh1FGoy00Oq8WEI2aI/93c3f0841fcf59743f57e238f6ed67aa/jake.png");
    }

    @Test
    public void whenAskForNotExistingAssetContentThenNotPresent() throws Exception {
        Optional<CmsPage> content = waitAndGet(contentfulCmsService.page("jacke", SUPPORTED_LOCALES));
        assertThat(content).isPresent();
        assertThat(content.get().fieldOrEmpty("pageContent.notExistingAsset")).isEmpty();

    }

    private <T> T waitAndGet(final CompletionStage<T> stage) throws InterruptedException, ExecutionException, TimeoutException {
        return stage.toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    private static String getValueForEnvVar(final String key) {
        return Optional.ofNullable(System.getenv(key))
                .orElseThrow(() -> new RuntimeException(
                        "Missing environment variable " + key + ", please provide the following environment variables for the integration test:\n" +
                                "export " + IT_CF_SPACE_ID + "=\"Your Contentful project key\"\n" +
                                "export " + IT_CF_TOKEN + "=\"Your Contentful authentication token\"\n"));
    }

}