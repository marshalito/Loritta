package com.mrpowergamerbr.loritta.website.views.subviews

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.set
import com.google.gson.JsonObject
import com.mrpowergamerbr.loritta.Loritta
import com.mrpowergamerbr.loritta.Loritta.Companion.GSON
import com.mrpowergamerbr.loritta.utils.encodeToUrl
import com.mrpowergamerbr.loritta.utils.jsonParser
import com.mrpowergamerbr.loritta.oauth2.TemmieDiscordAuth
import com.mrpowergamerbr.loritta.website.LorittaWebsite
import org.jooby.Request
import org.jooby.Response
import java.util.*

abstract class ProtectedView : AbstractView() {
	override fun handleRender(req: Request, res: Response, path: String, variables: MutableMap<String, Any?>): Boolean {
		if (path.startsWith("/dashboard")) {
			val state = req.param("state")
			if (!req.param("code").isSet) {
				if (!req.session().get("discordAuth").isSet) {
					val state = JsonObject()
					state["redirectUrl"] = LorittaWebsite.Companion.WEBSITE_URL.substring(0, LorittaWebsite.Companion.WEBSITE_URL.length - 1) + req.path()
					res.redirect(Loritta.config.authorizationUrl + "&state=${Base64.getEncoder().encodeToString(state.toString().toByteArray()).encodeToUrl()}")
					return false
				}
			} else {
				val code = req.param("code").value()
				val auth = TemmieDiscordAuth(code, "https://loritta.website/dashboard", Loritta.config.clientId, Loritta.config.clientSecret).apply {
					debug = false
				}
				auth.doTokenExchange()

				req.session()["discordAuth"] = GSON.toJson(auth)
				if (state.isSet) {
					// state = base 64 encoded JSON
					val decodedState = Base64.getDecoder().decode(state.value()).toString(Charsets.UTF_8)
					val jsonState = jsonParser.parse(decodedState).obj
					val redirectUrl = jsonState["redirectUrl"].nullString

					if (redirectUrl != null) {
						res.redirect(redirectUrl)
						return true
					}
				}

				// Caso o usuário utilizou o invite link que adiciona a Lori no servidor, terá o parâmetro "guild_id" na URL
				// Se o parâmetro exista, redirecione automaticamente para a tela de configuração da Lori
				val guildId = req.param("guild_id")
				if (guildId.isSet) {
					res.redirect("https://loritta.website/us/dashboard/configure/${guildId.value()}")
					return true
				}

				res.redirect("https://loritta.website/dashboard") // Redirecionar para a dashboard, mesmo que nós já estejamos lá... (remove o "code" da URL)
			}
			return true
		}
		return false
	}

	override fun render(req: Request, res: Response, path: String, variables: MutableMap<String, Any?>): String {
		if (!req.session().isSet("discordAuth")) { // Caso discordAuth não exista, vamos redirecionar para a tela de autenticação
			res.redirect(Loritta.config.authorizationUrl)
			return "Redirecionando..."
		}

		val discordAuth = GSON.fromJson<TemmieDiscordAuth>(req.session()["discordAuth"].value())
		try {
			discordAuth.isReady(true)
		} catch (e: Exception) {
			req.session().unset("discordAuth")
			res.redirect(Loritta.config.authorizationUrl)
			return "Redirecionando..."
		}
		variables["discordAuth"] = discordAuth
		return renderProtected(req, res, path, variables, discordAuth)
	}

	abstract fun renderProtected(req: Request, res: Response, path: String, variables: MutableMap<String, Any?>, discordAuth: TemmieDiscordAuth): String
}