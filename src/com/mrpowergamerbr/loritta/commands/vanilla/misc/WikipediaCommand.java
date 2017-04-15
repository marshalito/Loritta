package com.mrpowergamerbr.loritta.commands.vanilla.misc;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mrpowergamerbr.loritta.Loritta;
import com.mrpowergamerbr.loritta.commands.CommandBase;
import com.mrpowergamerbr.loritta.commands.CommandCategory;
import com.mrpowergamerbr.loritta.commands.CommandContext;
import com.mrpowergamerbr.loritta.utils.correios.EncomendaResponse;
import com.mrpowergamerbr.loritta.utils.correios.EncomendaResponse.PackageUpdate;

import net.dv8tion.jda.core.EmbedBuilder;

public class WikipediaCommand extends CommandBase {
	@Override
	public String getLabel() {
		return "wikipedia";
	}

	@Override
	public String getDescription() {
		return "Mostra uma versão resumida de uma página do Wikipedia";
	}

	public String getUsage() {
		return "[linguagem] conteúdo";
	}

	public List<String> getExample() {
		return Arrays.asList("Minecraft", "[en] Shantae");
	}

	public Map<String, String> getDetailedUsage() {
		return ImmutableMap.<String, String>builder()
				.put("linguagem", "*(Opcional)* Código de linguagem para procurar no Wikipédia, entre [], por padrão ele irá procurar na Wikipedia de Portugal [pt]")
				.put("conteúdo", "O que você deseja procurar no Wikipédia")
				.build();
	}

	@Override
	public CommandCategory getCategory() {
		return CommandCategory.MISC;
	}

	@Override
	public void run(CommandContext context) {
		if (context.getArgs().length >= 1) {
			String languageId = "pt";
			String inputLanguageId = context.getArgs()[0];
			boolean hasValidLanguageId = false;
			if (inputLanguageId.startsWith("[") && inputLanguageId.endsWith("]")) {
				languageId = inputLanguageId.substring(1, inputLanguageId.length() - 1);
				hasValidLanguageId = true;
			}
			try {
				String query = StringUtils.join(context.getArgs(), " ", hasValidLanguageId ? 1 : 0, context.getArgs().length);
				String wikipediaResponse = HttpRequest.get("https://" + languageId + ".wikipedia.org/w/api.php?format=json&action=query&prop=extracts&redirects=1&exintro=&explaintext=&titles=" + query).body();

				// Resolvi usar JsonParser em vez de criar um objeto para o Gson desparsear... a response do Wikipedia é meio "estranha"
				JsonObject wikipedia = new JsonParser().parse(wikipediaResponse).getAsJsonObject(); // Base
				JsonObject wikiQuery = wikipedia.getAsJsonObject("query"); // Query
				JsonObject wikiPages = wikiQuery.getAsJsonObject("pages"); // Páginas
				Entry<String, JsonElement> entryWikiContent = wikiPages.entrySet().iterator().next(); // Conteúdo
				
				if (entryWikiContent.getKey().equals("-1")) { // -1 = Nenhuma página encontrada
					context.sendMessage(context.getAsMention(true) + "Não consegui encontrar nada relacionado á **" + query + "** 😞");
				} else {
					// Se não é -1, então é algo que existe! Yay!
					String pageTitle = entryWikiContent.getValue().getAsJsonObject().get("title").getAsString();
					String pageExtract = entryWikiContent.getValue().getAsJsonObject().get("extract").getAsString();
					
					EmbedBuilder embed = new EmbedBuilder()
							.setTitle(pageTitle, null)
							.setColor(Color.BLUE)
							.setDescription(pageExtract);
					
					context.sendMessage(embed.build()); // Envie a mensagem!
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				context.sendMessage(context.getAsMention(true) + "**Deu ruim!**");
			}
		} else {
			context.explain();
		}
	}
}