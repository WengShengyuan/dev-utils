package org.wsy.devutils.io;

import com.alibaba.fastjson.JSON;
import org.wsy.devutils.io.wrapper.RawResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * HTTP封装
 * @author WSY
 *
 */
public class Http {
	
	private static final Logger logger = LoggerFactory.getLogger(Http.class);
	
	private static PoolingHttpClientConnectionManager cManager = null;
	private static int socketTimeout = 15 * 1000;
	private static int connectTimeout = 15 * 1000;
	private static int retryTime = 5;
	private static int maxTotal = 200;
	private static int maxPerRoute = 10;
	private static int validateAfterInactivity = 5 * 1000;
	private static int closeIdleTimeout = 15*1000;
	Properties properties=null;
	private static String utf8 = "UTF-8";

	/**
	 * 构造函数
	 */
	public Http() {
		
		SSLContext ctx=null;
		LayeredConnectionSocketFactory sslsf = null;
		boolean bullSSL = false;
		logger.info("reading http properties from file: util-dev-utils-collection.properties ...");
		try (InputStream fis = Http.class.getClassLoader().getResourceAsStream("util-dev-utils-collection.properties")){
			properties = PropertiesUtil.readProperties(fis);
			bullSSL = Boolean.valueOf(properties.getProperty("io.client.bullssl"));
			socketTimeout = Integer.valueOf(properties.getProperty("io.http.socket.timeout"));
			connectTimeout = Integer.valueOf(properties.getProperty("io.http.connect.timeout"));
			retryTime = Integer.valueOf(properties.getProperty("io.http.retry"));
			maxTotal = Integer.valueOf(properties.getProperty("io.http.maxtotal"));
			maxPerRoute = Integer.valueOf(properties.getProperty("io.http.maxperroute"));
			validateAfterInactivity = Integer.valueOf(properties.getProperty("io.http.validateafterinactivity"));
			closeIdleTimeout = Integer.valueOf(properties.getProperty("io.http.closeidletimeout"));
		}  catch (Exception e) {
			logger.error("fail to read client properties set to default. {}",e.getMessage());
		}
		
		if(bullSSL){
			logger.info("start bull ssl context.");
			try {
				ctx = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
					@Override
					public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
						return true;
					}
				}).build();
			} catch (KeyManagementException e) {
				logger.error("fail to create ssl context builder.",e);
			} catch (NoSuchAlgorithmException e) {
				logger.error("fail to create ssl context builder.",e);
			} catch (KeyStoreException e) {
				logger.error("fail to create ssl context builder.",e);
			}
			HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
			sslsf = new SSLConnectionSocketFactory(ctx, hostnameVerifier);
		} else {
			try {
				sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault());
			} catch (NoSuchAlgorithmException e) {
				logger.error("fail to create ssl context builder.",e);
			}
		}
		
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
				.register("https", sslsf).register("http", new PlainConnectionSocketFactory()).build();
		cManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		cManager.setMaxTotal(maxTotal);
		cManager.setDefaultMaxPerRoute(maxPerRoute);
		cManager.setValidateAfterInactivity(validateAfterInactivity);
		cManager.closeIdleConnections(closeIdleTimeout, TimeUnit.MILLISECONDS);
	}

	/**
	 * 获取http客户端
	 * @return
	 */
	public CloseableHttpClient getHttpClient() {
		return HttpClients.custom().setConnectionManager(cManager)
				.setDefaultRequestConfig(
						RequestConfig.custom().setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).build())
				.setRetryHandler(new DefaultHttpRequestRetryHandler(retryTime, true)).build();
	}


	/**
	 * 发起get请求
	 * @param url 请求地址
	 * @return 请求响应
	 * @throws Exception 异常
	 */
	public String get(String url) throws Exception {
		logger.info("start http get: "+url);
		CloseableHttpClient client = getHttpClient();
		CloseableHttpResponse response = null;
		HttpGet httpGet = new HttpGet();
		try {
			httpGet.setURI(new URI(url));
			httpGet.addHeader("Connection", "close");
			response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			if (null != entity) {
				if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
					String content = EntityUtils.toString(entity,utf8);
					logger.debug("http get response: "+content);
					return content;
				} else {
					throw new Exception("http return status["+response.getStatusLine().getStatusCode()+"]: "+response.getStatusLine().getReasonPhrase());
				}
			} else {
				throw new Exception("http response entity is null");
			}
		} finally {
			if (null != response) {
				EntityUtils.consume(response.getEntity());
				response.close();
			}
			httpGet.releaseConnection();
		}
	}

	
	public RawResponse rawGet(String url) throws Exception {
		logger.info("start http get: "+url);
		CloseableHttpClient client = getHttpClient();
		CloseableHttpResponse response = null;
		HttpGet httpGet = new HttpGet();
		try {
			httpGet.setURI(new URI(url));
			httpGet.addHeader("Connection", "close");
			response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			RawResponse rawResponse = new RawResponse();
			rawResponse.setHttpCode(response.getStatusLine().getStatusCode());
			rawResponse.setReasonPhase(response.getStatusLine().getReasonPhrase());
			rawResponse.setResponseContent((entity==null)?"":EntityUtils.toString(entity, utf8));
			return rawResponse;
		} finally {
			if (null != response) {
				EntityUtils.consume(response.getEntity());
				response.close();
			}
			httpGet.releaseConnection();
		}
	}
	
	public RawResponse rawJsonPost(String url,Object body) throws Exception {
		String jsonString = JSON.toJSONString(body);
		logger.info("start http post. url:"+url+", body:"+jsonString);
		CloseableHttpClient client = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost post;
		post = new HttpPost(url);
		post.addHeader("Connection", "close");
		post.addHeader(HTTP.CONTENT_TYPE, "application/json");
		StringEntity se = new StringEntity(jsonString,ContentType.APPLICATION_JSON);
		post.setEntity(se);
		return getRawResponse(client, response, post);
	}

	private RawResponse getRawResponse(CloseableHttpClient client, CloseableHttpResponse response, HttpPost post) throws IOException, Exception {
		try{
			response = client.execute(post);
			HttpEntity entity = response.getEntity();
			RawResponse rawResponse = new RawResponse();
			rawResponse.setHttpCode(response.getStatusLine().getStatusCode());
			rawResponse.setReasonPhase(response.getStatusLine().getReasonPhrase());
			rawResponse.setResponseContent((entity==null)?"": EntityUtils.toString(entity, utf8));
			return rawResponse;
		} finally {
			if (null != response) {
				EntityUtils.consume(response.getEntity());
				response.close();
			}
			if(null!=post){
				post.releaseConnection();
			}
		}
	}

	public String jsonPost(String url,Object body) throws Exception {
		String jsonString = JSON.toJSONString(body);
		logger.info("start http post. url:"+url+", body:"+jsonString);
		CloseableHttpClient client = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost post = null;
		post = new HttpPost(url);
		post.addHeader("Connection", "close");
		post.addHeader(HTTP.CONTENT_TYPE, "application/json");
		StringEntity se = new StringEntity(jsonString,ContentType.APPLICATION_JSON);
		post.setEntity(se);
		return getPostString(client, response, post);
	}

	private String getPostString(CloseableHttpClient client, CloseableHttpResponse response, HttpPost post) throws Exception {
		try{
			response = client.execute(post);
			return parseResponse(response);
		} catch (Exception e) {
			throw e;
		} finally {
			if (null != response) {
				EntityUtils.consume(response.getEntity());
				response.close();
			}
			if(null!=post){
				post.releaseConnection();
			}
		}
	}

	private String parseResponse(CloseableHttpResponse response) throws Exception {
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            HttpEntity httpEntity = response.getEntity();
            if (httpEntity != null) {
                String content = EntityUtils.toString(httpEntity,utf8);
                logger.debug("http post return: "+content);
                return content;
            } else {
                throw new Exception("http response entity is null");
            }
        } else {
            throw new Exception("http return status["+response.getStatusLine().getStatusCode()+"]: "+response.getStatusLine().getReasonPhrase());
        }
	}

	public RawResponse rawPost(String url,Map<String, String> map) throws Exception {
		logger.info("start http post. url:"+url+", params:"+map2String(map));
		CloseableHttpClient client = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost post = null;
		post = postRequest(url, map);
		return getRawResponse(client, response, post);
	}

	private HttpPost postRequest(String url, Map<String, String> map) throws UnsupportedEncodingException {
		HttpPost post;
		try {
			post = new HttpPost(url);
			post.addHeader("Connection", "close");
			if (map != null && map.size() > 0) {
				List<NameValuePair> list = new ArrayList<NameValuePair>();
				for (String key : map.keySet()) {
					list.add(new BasicNameValuePair(key, map.get(key)));
				}
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, utf8);
				post.setEntity(entity);
			}
		} catch (UnsupportedEncodingException e) {
			throw e;
		}
		return post;
	}


	public String post(String url, Map<String, String> map) throws Exception {
		logger.info("start http post. url:"+url+", params:"+map2String(map));
		CloseableHttpClient client = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost post = postRequest(url, map);
		return getPostString(client, response, post);
	}
	
	
	public RawResponse rawPlainPost(String url,Object body) throws Exception {
		logger.info("start http binary post. url:"+url+", params:"+body);
		CloseableHttpClient client = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost post = null;
		try {
			post = new HttpPost(url);
			post.addHeader("Connection", "close");
			StringEntity stringEntity = new StringEntity(body.toString(), utf8);
			post.setEntity(stringEntity);
			response = client.execute(post);
			HttpEntity entity = response.getEntity();
			RawResponse rawResponse = new RawResponse();
			rawResponse.setHttpCode(response.getStatusLine().getStatusCode());
			rawResponse.setReasonPhase(response.getStatusLine().getReasonPhrase());
			rawResponse.setResponseContent((entity==null)?"":EntityUtils.toString(entity, utf8));
			return rawResponse;
		} catch (Exception e) {
			throw e;
		} finally {
			if (null != response) {
				EntityUtils.consume(response.getEntity());
				response.close();
			}
			if(null!=post){
				post.releaseConnection();
			}
		}
	}
	
	
	public String plainPost(String url,Object body) throws Exception {
		logger.info("start http binary post. url:"+url+", params:"+body);
		CloseableHttpClient client = getHttpClient();
		CloseableHttpResponse response = null;
		HttpPost post = null;
		try {
			post = new HttpPost(url);
			post.addHeader("Connection", "close");
			StringEntity stringEntity = new StringEntity(body.toString(), utf8);
			post.setEntity(stringEntity);
			response = client.execute(post);
			return parseResponse(response);
		} catch (Exception e) {
			throw e;
		} finally {
			if (null != response) {
				EntityUtils.consume(response.getEntity());
				response.close();
			}
			if(null!=post){
				post.releaseConnection();
			}
		}
	}
	
	public void download(String url,File destFile) throws Exception {
		CloseableHttpClient client = getHttpClient();
		CloseableHttpResponse response = null;
		HttpGet get = null;
		try {
			get = new HttpGet(url);
			response = client.execute(get);
			HttpEntity entity = response.getEntity();  
			InputStream in = entity.getContent();  
			long length=entity.getContentLength();  
			if(length<=0){  
			    throw new Exception("no file get from server");
			}  
			OutputStream out = new FileOutputStream(destFile);  
			saveTo(in, out);
		} catch (Exception e) {
			throw e;
		} finally {
			if(null!=response){
				EntityUtils.consume(response.getEntity());
				response.close();
			}
			if(null!=get){
				get.releaseConnection();
			}
		}
	}
	
	private void saveTo(InputStream in, OutputStream out) {
        try {
			byte[] data = new byte[1024*1024];  
			int index =0;  
			while ((index=in.read(data) )!= -1) {  
			    out.write(data,0,index);  
			}  
			in.close();  
			out.close();
		} catch (Exception e) {
			logger.error("save file fail.",e);
		} finally {
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			if(out!=null){
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
    }  
	
	private String map2String(Map<String, String> map) {
		if(map.keySet().size()>0){
			StringBuilder sBuilder=new StringBuilder("[");
			for(String key:map.keySet()){
				sBuilder.append(key).append(":").append(map.get(key)).append(",");
			}
			return sBuilder.substring(0,sBuilder.length()-1)+"]";
		} else {
			return "[]";
		}
	}
	
}
