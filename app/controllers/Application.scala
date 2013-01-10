package controllers

import collection.JavaConversions._
import com.codahale.jerkson.Json._
import com.factual.driver._
import fi.foyt.foursquare.api._
import java.net.URLEncoder
import play.api.Play.current
import play.api._
import play.api.cache.{EhCachePlugin, Cache}
import play.api.data.Forms._
import play.api.data._
import play.api.libs.concurrent._
import play.api.libs.iteratee._
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.templates._

//import models._
import views._

object Application extends Controller {
  import play.api.Logger._

  def config(key: String) =
    Play.application.configuration.getString(key).get

  lazy val LocuKey = config("locu.apikey")

  lazy val LocuUrlBase = "http://api.locu.com/v1_0/venue/%s/?api_key=" + LocuKey

  lazy val factual =
    new Factual(config("factual.publickey"), config("PIyqqfwuYkLjVFw5XY77wm9WpL1nN9H4GdIi8v1S"))

  lazy val apiUrl = "https://api.foursquare.com/v2/"

  def buildApiReq(path: String, oauth: String, params: String): Promise[Option[String]] = {
    val _url = apiUrl + path + "?oauth_token=" + oauth + "&v=20120909" + params
    info(_url)
    WS.url(_url).post(Map.empty[String, Seq[String]]) map { r => (r.json \\ "id").headOption.map { _.as[String] } }
  }

  def buildApiReqTuple(path: String, oauth: String, params: String): Promise[(String, String)] = {
    val _url = apiUrl + path + "?oauth_token=" + oauth + "&v=20120909" + params
    info(_url)
    _url
    WS.url(_url).post(Map.empty[String, Seq[String]]) map { r =>
      ((r.json \\ "id")(0).as[String], (r.json \\ "canonicalUrl")(0).as[String])
    }
  }

  def foursquare() = {
    new FoursquareApi(config("4sq.publickey"), config("4sq.secretkey"), config("4sq.callback"))
  }

  def fetch(factualId: String) = {
    factual.fetch(
      "crosswalk",
      new Query().field("namespace").equal("foursquare")
      .field("factual_id").equal(factualId)).first().get("namespace_id").toString
  }

  def getFoursquareId(factualId: String): Promise[Option[String]] = {
    Akka.future {
      try {
        Option(fetch(factualId))
      } catch { case e =>
        None
      }
    }
  }

  def getCheckins = Action { implicit request =>
    val foursquareApi = foursquare()
    Async {

      // get list of foursquare checkin ids
      session.get("foursquare") map { token =>
        foursquareApi.setoAuthToken(token)

        val checkinsFt: Seq[Promise[Option[String]]] =
          foursquareApi.usersCheckins(null, null, null, null, null).getResult.getItems.map { i =>
            locuSearch(i.getVenue.getName, i.getVenue.getLocation.getPostalCode)
          }

        Promise.sequence(checkinsFt) flatMap { results =>
          val checkins: Set[String] = results.flatten.toSet.asInstanceOf[Set[String]]

          info("checkins: " + checkins)

          // get similar venues filter out duplicates
          Promise.sequence(checkins map { locuVenueDetailsSimilarVenues(_) }) flatMap { rs =>
            val similar = rs.flatten.filter { v: String => !checkins(v) }.groupBy(x => x ).map { case (num, o) =>
              (num, o.size)
            }.toSeq.sortWith { (a, b) => a._2 > b._2 }

            info("similar: " + similar)

            Promise.sequence(similar map { idPair => locuVenueDetailsFactualId(idPair._1) map { v =>
              v map { getFoursquareId(_) } } }) map { _.flatten }
          }
        } flatMap { foursquareVenues =>
          Promise.sequence(foursquareVenues).flatMap { venues =>
            buildApiReqTuple("lists/add", token, "&name=" + URLEncoder.encode("recommendations", "UTF-8")) flatMap { case (id, url) =>
              Promise.sequence(venues.flatten.take(6).map { vid =>
                buildApiReq("lists/" + id + "/additem", token, "&venueId=" + vid) }) map { _ =>
                if (venues.isEmpty) {
                  Ok(html.error("We found no revenues to recommend"))
                } else {
                  Ok(html.locu(id, url))
                }
              }
            }
          }
        }
      } getOrElse {
        Akka.future { Redirect(foursquareApi.getAuthenticationUrl()) }
      }
    }
  }

  def foursquareAuth = Action {
      // First we need to redirect our user to authentication page.
    Redirect(foursquare().getAuthenticationUrl())
  }

  def callback(code: String) = Action { implicit request =>
    try {
      val foursquareApi = foursquare()
      // finally we need to authenticate that authorization code
      info("AUth code = " + code)
      foursquareApi.authenticateCode(code)
      Redirect(routes.Application.getCheckins).withSession(
        "foursquare" -> foursquareApi.getOAuthToken
      )
      // ... and voilà we have a authenticated Foursquare client
    } catch { case e =>
      Ok(html.error("auth fail " + e.toString))
    }
  }

  def locuVenueDetailsSimilarVenues(id: String): Promise[Seq[String]] = {
    val _url = LocuUrlBase.format(id)
    info("locu id = " + id)
    WS.url(_url).get() map { r => (r.json \\ "similar_venues")(0).as[Seq[String]] }
  }

  def locuVenueDetailsFactualId(id: String): Promise[Option[String]] = {
    val _url = LocuUrlBase.format(id)
    WS.url(_url).get() map { r =>
      try {
        (r.json \\ "factual_id").headOption map { _.as[String] }
      } catch { case _ =>
        None
      }
    }
  }

  def locuSearch(name: String, postalCode: String): Promise[Option[String]] = {
    val _url = LocuUrlBase.format("search") + "&name=" + URLEncoder.encode(name, "UTF-8") + "&postal_code=" + postalCode
    WS.url(_url).get().map { r => Option(r.json \\ "id") flatMap { _.headOption } map { _.as[String] } }
  }

  def home = Action { implicit request =>
    Ok(html.home("Home"))
  }
}
