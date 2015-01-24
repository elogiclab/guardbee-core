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
package com.elogiclab.guardbee.core.models

import org.joda.time.DateTime
import play.api.Play.current
import play.api.data.format.Formatter
import play.api.data.FormError
import com.elogiclab.guardbee.core.utils.TokenGenerator
import com.elogiclab.guardbee.core.plugins.AccessTokenPlugin

case class AccessToken(
  token: String = TokenGenerator.newToken,
  token_type: String,
  username: String,
  client_id: String,
  scope: String,
  token_expiration: DateTime = DateTime.now().plusSeconds(current.configuration.getInt("guardbee.tokenExpireIn").getOrElse(3600)),
  refresh_token: Option[String],
  refresh_token_expiration: DateTime = DateTime.now().plusSeconds(current.configuration.getInt("guardbee.refreshTokenExpireIn").getOrElse(604800))
) {
  def isTokenExpired = token_expiration.isBeforeNow
  def isRefreshTokenExpired = refresh_token_expiration.isBeforeNow
  def expire_in: Int = ((token_expiration.getMillis - DateTime.now.getMillis) / 1000).intValue
}

object AccessToken {
    import play.api.Play.current 
    private lazy val instance:AccessTokenPlugin = current.plugin[AccessTokenPlugin].getOrElse(sys.error("Plugin AccessTokenPlugin not loaded!"))
    
    def get(token: String): Option[AccessToken] = instance.get(token)
    def create(entity:AccessToken):Unit = instance.create(entity)

    
  implicit def accessTokenFormat: Formatter[AccessToken] = new Formatter[AccessToken] {
    def bind(key: String, data: Map[String, String]) = data.get(key).flatMap( { id => instance.get(id) }).toRight(Seq(FormError(key, "error.invalid_xxx", Nil)))
    def unbind(key: String, value: AccessToken) = Map(key -> value.token)
  }


}