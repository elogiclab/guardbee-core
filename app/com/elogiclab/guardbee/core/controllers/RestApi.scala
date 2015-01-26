/**
 * Copyright (c) 2015 Marco Sarti <marco.sarti at gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.elogiclab.guardbee.core.controllers

import play.api.mvc.Controller
import play.api.mvc.Request
import play.api.mvc.WrappedRequest
import play.api.mvc.AnyContent
import play.api.mvc.Result
import play.api.mvc.Action
import com.elogiclab.guardbee.core.JsonErrors
import play.api.mvc.RequestHeader
import com.elogiclab.guardbee.core.models.AccessTokenInfo




trait RestApi extends Controller with JsonErrors {
  
  case class Authentication(name: String, grantedAuthorities:Seq[String], grantedScopes:Seq[String])
  
  case class RestApiRequest[A](request: Request[A], authentication:Authentication) extends WrappedRequest(request)
  
  def AllOf(authzfn: ((=> Authentication) => Boolean) *)(authn: => Authentication): Boolean = {
    authzfn.size > 0 && authzfn.filter(p => p(authn)).size == authzfn.size
  }
  
  def HasRole(role: String)(authn: => Authentication): Boolean = authn.grantedScopes.contains(role)
  
  def Scope(scope: => String)(auth: => Authentication): Boolean = auth.grantedScopes.contains(scope)
  
  def verifyToken(authorization:(=> Authentication) => Boolean)(access_token:String, f: RestApiRequest[AnyContent] => Result)(implicit request:Request[AnyContent]):Result = {
    AccessTokenInfo.get(access_token)
    .filter { x => !x.isTokenExpired }
    .map { x => Authentication(x.username, Nil, x.scope.split(" ")) }
    .filter { x => authorization(x) }
    .map { x => f(RestApiRequest(request, x)) }
    .getOrElse(BadRequest(errors(Seq(("token", "error.invalid_auth_token")))))
  }
  
  def SecuredAction(authorization:(=> Authentication) => Boolean)(f: RestApiRequest[AnyContent] => Result) = Action { implicit request =>
    
    (request.getQueryString("access_token"), request.headers.get("Authorization")) match {
      case (Some(parm), Some(header)) => BadRequest(errors(Seq(("authentication", "error.multiple_auth_method"))))
      case (Some(parm), _) => verifyToken(authorization)(parm, f)
      case (_, Some(header)) => {
        Some(header).map { header => 
          header.trim.split(" ") 
        }
        .filter { array => array.length == 2 && array(0) == "Bearer" }
        .map { array => verifyToken(authorization)(array(1), f) }
        .getOrElse(BadRequest(errors(Seq(("token", "error.invalid_auth_token")))))
      }
      case (None, None) => Unauthorized(errors(Seq(("authentication", "error.no_token_specified"))))
    }
  }

}