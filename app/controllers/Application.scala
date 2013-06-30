package controllers

import play.api.libs.openid._
import play.api.libs.concurrent.Execution.Implicits._
import play.api._
import play.api.mvc._
import play.api.mvc.RequestHeader
import play.api.data.Form
import play.api.data.Forms.{single, nonEmptyText}
import scala.concurrent.Future
import models._
import models.User
import se.radley.plugin.salat.Binders.ObjectId
import java.util.Date


object Application extends Controller {

  def index = Action { implicit request =>
    session.get("_user_openid").map { _user_openid =>
      val user = User.findOneByOpenid(_user_openid).get
      Ok(views.html.index(user.username + "(" + user.email + ")さん かんぱるへようこそ！"))
    }.getOrElse {
      Ok(views.html.index("かんぱるー！"))
    }
  }

  def list = Action {
   Ok(views.html.index("list page"))
  }


//  def login = Action {
//    Ok(views.html.login())
//  }
  val loginForm = Form("openid" -> nonEmptyText)

  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }
  def loginPost = Action { implicit request =>
    Form(single(
      "openid" -> nonEmptyText
    )).bindFromRequest.fold(
      error => {
        Logger.info("bad request " + error.toString)
        BadRequest(error.toString)
      }, {
        case (openid) => AsyncResult {
          val url = OpenID.redirectURL(openid,
           routes.Application.openIDCallback.absoluteURL(),
           Seq("email" -> "http://schema.openid.net/contact/email",
               "last" -> "http://axschema.org/namePerson/last"))
          url.map(a => Redirect(a)).
           fallbackTo(Future(Redirect(routes.Application.login)))
        }
      }
    )
  }

  def openIDCallback = Action { implicit request =>
    AsyncResult(
      OpenID.verifiedId.map((info: UserInfo) => {
        //Ok(info.id + "\n" + info.attributes)).
        User.save(User(openid=info.id, username=info.attributes.getOrElse("last",""), email=info.attributes.getOrElse("email",""), updated = Option(new Date())))
        Ok(views.html.index(
            info.attributes("last") + "(" + info.attributes("email") + ")さん かんぱるへようこそ！")).withSession( session + ("_user_openid" -> info.id))})
        fallbackTo(Future(Forbidden))
      )
  }

  def logout = Action { implicit request =>
     Redirect(routes.Application.index).withNewSession.flashing(
      "success" -> "You've been logged out")
  }
}
