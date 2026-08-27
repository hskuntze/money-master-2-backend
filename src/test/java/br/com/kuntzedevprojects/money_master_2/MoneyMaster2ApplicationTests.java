package br.com.kuntzedevprojects.money_master_2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import br.com.kuntzedevprojects.money_master_2.config.bootstrap.DataBootstrap;
import br.com.kuntzedevprojects.money_master_2.services.BcbSgsService;
import br.com.kuntzedevprojects.money_master_2.services.FinanceChatService;

@SpringBootTest(properties = {
		"spring.ai.chat.client.enabled=false",
		"spring.ai.openai.chat.enabled=false",
		"spring.autoconfigure.exclude="
				+ "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration,"
				+ "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration,"
				+ "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration,"
				+ "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration,"
				+ "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration,"
				+ "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration"
})
class MoneyMaster2ApplicationTests {

	@MockitoBean
	private BcbSgsService bcbSgsService;

	@MockitoBean
	private FinanceChatService financeChatService;

	@MockitoBean
	private DataBootstrap dataBootstrap;

	@Test
	void contextLoads() {
	}

}
