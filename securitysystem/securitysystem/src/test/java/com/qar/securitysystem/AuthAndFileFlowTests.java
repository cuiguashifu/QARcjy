package com.qar.securitysystem;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.qar.securitysystem.model.PersonRecordEntity;
import com.qar.securitysystem.repo.PersonRecordRepository;
import com.qar.securitysystem.util.IdUtil;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "app.security.cookieName=QAR_SESSION",
                "app.security.cookieSecure=false",
                "app.security.cookieSameSite=Strict",
                "app.security.sessionTtlMinutes=60",
                "app.admin.username=admin",
                "app.admin.password=CAUCqar",
                "app.person.seedEnabled=false",
                "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
        }
)
public class AuthAndFileFlowTests {
    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private PersonRecordRepository personRecordRepository;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        this.mvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @Test
    void register_login_upload_download_admin_export() throws Exception {
        PersonRecordEntity pr = new PersonRecordEntity();
        pr.setId(IdUtil.newId());
        pr.setPersonNo("user1");
        pr.setFullName("User One");
        pr.setIdLast4("1234");
        pr.setPhone("13800000000");
        pr.setDepartment("飞行一部");
        pr.setAirline("CAUC");
        pr.setPositionTitle("机长");
        pr.setCreatedAt(Instant.now());
        personRecordRepository.save(pr);

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"emailOrUsername\":\"admin\",\"fullName\":\"x\",\"idLast4\":\"0000\",\"password\":\"x\",\"passwordConfirm\":\"x\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"emailOrUsername\":\"user1\",\"fullName\":\"User One\",\"idLast4\":\"1234\",\"contact\":\"13800000000\",\"airline\":\"CAUC\",\"positionTitle\":\"机长\",\"department\":\"飞行一部\",\"password\":\"Passw0rd!\",\"passwordConfirm\":\"Passw0rd!\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"emailOrUsername\":\"user1\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isForbidden());

        String adminSetCookie = mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"emailOrUsername\":\"admin\",\"password\":\"CAUCqar\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        assertThat(adminSetCookie).contains("QAR_SESSION=");
        String adminToken = adminSetCookie.split("QAR_SESSION=")[1].split(";", 2)[0];
        Cookie adminCookie = new Cookie("QAR_SESSION", adminToken);

        String reqList = mvc.perform(get("/api/admin/account-requests").cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(reqList).contains("\"personNo\":\"user1\"");
        String reqId = reqList.split("\"id\":\"")[1].split("\"", 2)[0];

        mvc.perform(post("/api/admin/account-requests/" + reqId + "/approve")
                        .cookie(adminCookie)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"adminNote\":\"ok\"}"))
                .andExpect(status().isOk());

        String setCookie = mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"emailOrUsername\":\"user1\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie).contains("QAR_SESSION=");
        String token = setCookie.split("QAR_SESSION=")[1].split(";", 2)[0];
        Cookie sessionCookie = new Cookie("QAR_SESSION", token);

        byte[] payload = "hello-qar".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", payload);

        mvc.perform(multipart("/api/files")
                        .file(file)
                        .param("policy", "role:user")
                        .cookie(sessionCookie)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        String fileJson = mvc.perform(multipart("/api/files")
                        .file(file)
                        .param("policy", "role:user")
                        .param("personNo", "user1")
                        .cookie(adminCookie)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(fileJson).contains("\"id\"");
        String fileId = fileJson.split("\"id\":\"")[1].split("\"", 2)[0];

        byte[] downloaded = mvc.perform(get("/api/files/" + fileId + "/download")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        assertThat(downloaded).isEqualTo(payload);

        mvc.perform(post("/api/feedback")
                        .cookie(sessionCookie)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"type\":\"bug\",\"subject\":\"下载校验\",\"message\":\"下载正常，但我想确认一下流程\",\"relatedFileId\":\"" + fileId + "\"}"))
                .andExpect(status().isOk());

        String feedbackList = mvc.perform(get("/api/admin/feedback").cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(feedbackList).contains("下载校验");
        String feedbackId = feedbackList.split("\"id\":\"")[1].split("\"", 2)[0];

        mvc.perform(patch("/api/admin/feedback/" + feedbackId)
                        .cookie(adminCookie)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"status\":\"RESOLVED\",\"adminReply\":\"收到，流程正常\"}"))
                .andExpect(status().isOk());

        byte[] zipBytes = mvc.perform(get("/api/admin/files/export").cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        boolean found = false;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith("_a.txt")) {
                    byte[] buf = zis.readAllBytes();
                    assertThat(buf).isEqualTo(payload);
                    found = true;
                    break;
                }
            }
        }
        assertThat(found).isTrue();
    }
}
