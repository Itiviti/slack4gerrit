package com.ullink.slack.review;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class HttpHelper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpHelper.class);

    private static ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(8));

    private static class HttpDataRetriever implements Callable<String>
    {

        URL    url;
        String user;
        String password;

        public HttpDataRetriever(URL url, String user, String password)
        {
            this.url = url;
            this.user = user;
            this.password = password;
        }

        @Override
        public String call() throws Exception
        {
            return getFromHttp(url, user, password);
        }

    }

    public static ListenableFuture<String> getAsyncFromHttp(URL url)
    {
        return getAsyncFromHttp(url, null, null);
    }

    public static ListenableFuture<String> getAsyncFromHttp(URL url, String user, String password)
    {
        HttpDataRetriever retriever = new HttpDataRetriever(url, user, password);
        return executor.submit(retriever);
    }

    public static String getFromHttp(URL url) throws IOException
    {
        return getFromHttp(url, null, null);
    }

    public static String getFromHttp(URL url, String user, String password) throws IOException
    {
        LOGGER.debug("Fetching data from URL: " + url);

        HttpClientContext context = HttpClientContext.create();
        if (user != null)
        {

            HttpHost targetHost = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
            AuthCache authCache = new BasicAuthCache();
            authCache.put(targetHost, new BasicScheme());

            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
            context.setCredentialsProvider(credentialsProvider);
            context.setAuthCache(authCache);
        }
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url.toExternalForm());
        HttpResponse response = client.execute(request, context);
        LOGGER.debug("Response status code: " + response.getStatusLine().getStatusCode());
        InputStreamReader streamReader = new InputStreamReader(response.getEntity().getContent());
        String data = CharStreams.toString(streamReader);
        streamReader.close();
        return data;

    }

}
