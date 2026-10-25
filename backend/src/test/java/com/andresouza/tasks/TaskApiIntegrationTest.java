package com.andresouza.tasks;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TaskApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void createsMovesAndSummarizesTasks() throws Exception {
        String token = registerAndLogin();
        String yesterday = LocalDate.now().minusDays(1).toString();

        Integer late = createTask(token, "Enviar relatório", "HIGH", yesterday);
        Integer other = createTask(token, "Revisar PR", "MEDIUM", null);

        mvc.perform(patch("/api/tasks/{id}/status", other).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").exists());

        mvc.perform(get("/api/tasks/summary").header("Authorization", token))
                .andExpect(jsonPath("$.todo").value(1))
                .andExpect(jsonPath("$.done").value(1))
                .andExpect(jsonPath("$.overdue").value(1));

        mvc.perform(get("/api/tasks").param("status", "TODO").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(late))
                .andExpect(jsonPath("$.content[0].overdue").value(true));

        mvc.perform(get("/api/tasks").param("search", "revisar").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(put("/api/tasks/{id}", late).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Enviar relatório mensal\", \"priority\": \"LOW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Enviar relatório mensal"))
                .andExpect(jsonPath("$.overdue").value(false));

        mvc.perform(delete("/api/tasks/{id}", late).header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void usersOnlySeeTheirOwnTasks() throws Exception {
        String ana = registerAndLogin();
        String bruno = registerAndLogin();
        Integer taskId = createTask(ana, "Tarefa da Ana", "LOW", null);

        mvc.perform(get("/api/tasks").header("Authorization", bruno))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(delete("/api/tasks/{id}", taskId).header("Authorization", bruno))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthenticationAndValidPayload() throws Exception {
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());

        String token = registerAndLogin();
        mvc.perform(post("/api/tasks").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.priority").exists());
    }

    private Integer createTask(String token, String title, String priority, String dueDate) throws Exception {
        String due = dueDate == null ? "null" : "\"" + dueDate + "\"";
        String body = mvc.perform(post("/api/tasks").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"%s\", \"priority\": \"%s\", \"dueDate\": %s}".formatted(title, priority, due)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String registerAndLogin() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Usuário\", \"email\": \"%s\", \"password\": \"senha-segura\"}".formatted(email)))
                .andExpect(status().isCreated());
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"senha-segura\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Usuário"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }
}
