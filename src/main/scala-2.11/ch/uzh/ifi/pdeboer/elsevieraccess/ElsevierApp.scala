package ch.uzh.ifi.pdeboer.elsevieraccess

import java.net.URI

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.{BasicCookieStore, HttpClientBuilder}
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.util.EntityUtils
import play.api.libs.json.Json

/**
 * Created by mattia on 17.09.15.
 */
object ElsevierApp extends App with LazyLogging{

  val config = ConfigFactory.load()

  val key = ConfigFactory.load("apiKey.conf").getString("key")
  logger.debug("Key: " + key)

  val httpClient = HttpClientBuilder.create().build()
  val httpContext = new BasicHttpContext()

  val cookieStore = new BasicCookieStore()
  httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore)

  val searchString = "all(anova)"
  
  val answer = Json.parse(get(s"${config.getString("search")}%20and%20$searchString&APIKey=$key"))
  val urls = (answer \\ "dc:identifier").map(_.toString.replaceAll("\"", ""))
  urls.foreach(url => {
    logger.debug(config.getString("getPdf")+url+"?httpAccept=application/pdf&view=FULL&APIKey="+key)
  })

  httpClient.close()
  httpContext.clear()

  def get(url: String): String = {
    logger.info(s"Executing query: $url")
    val request = new HttpGet(url)
    val response = httpClient.execute(request, httpContext).getEntity
    val content = EntityUtils.toString(response)
    EntityUtils.consumeQuietly(response)
    content
  }
}


