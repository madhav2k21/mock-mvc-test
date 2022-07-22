package com.luv2code.springmvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luv2code.springmvc.models.CollegeStudent;
import com.luv2code.springmvc.models.MathGrade;
import com.luv2code.springmvc.repository.HistoryGradesDao;
import com.luv2code.springmvc.repository.MathGradesDao;
import com.luv2code.springmvc.repository.ScienceGradesDao;
import com.luv2code.springmvc.repository.StudentDao;
import com.luv2code.springmvc.service.StudentAndGradeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource("/application-test.properties")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class GradebookControllerTest {
    private static final MediaType APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON;
    private static MockHttpServletRequest request;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CollegeStudent student;
    @Mock
    private StudentAndGradeService studentAndGradeService;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private StudentDao studentDao;
    @Autowired
    private MathGradesDao mathGradeDao;
    @Autowired
    private ScienceGradesDao scienceGradeDao;
    @Autowired
    private HistoryGradesDao historyGradeDao;
    @Autowired
    private StudentAndGradeService studentService;
    @Value("${sql.script.create.student}")
    private String sqlAddStudent;
    @Value("${sql.script.create.math.grade}")
    private String sqlAddMathGrade;
    @Value("${sql.script.create.science.grade}")
    private String sqlAddScienceGrade;
    @Value("${sql.script.create.history.grade}")
    private String sqlAddHistoryGrade;
    @Value("${sql.script.delete.student}")
    private String sqlDeleteStudent;
    @Value("${sql.script.delete.math.grade}")
    private String sqlDeleteMathGrade;
    @Value("${sql.script.delete.science.grade}")
    private String sqlDeleteScienceGrade;
    @Value("${sql.script.delete.history.grade}")
    private String sqlDeleteHistoryGrade;

    @BeforeAll
    public static void setup() {
        request = new MockHttpServletRequest();
        request.setParameter("firstname", "madhav");
        request.setParameter("lastname", "anupoju");
        request.setParameter("emailAddress", "madhav@techm");
    }

    @BeforeEach
    public void setupDatabase() {
        jdbc.execute(sqlAddStudent);
        jdbc.execute(sqlAddMathGrade);
        jdbc.execute(sqlAddScienceGrade);
        jdbc.execute(sqlAddHistoryGrade);
    }

    @Test
    void getStudentHttpRequest() throws Exception {

        student.setFirstname("dill");
        student.setFirstname("lastname");
        student.setEmailAddress("dill@us");
        entityManager.persist(student);
       mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(2)));

    }

    @Test
    void createSudentHttpRequest() throws Exception {
        student.setFirstname("dill");
        student.setFirstname("lastname");
        student.setEmailAddress("dill@us123");
       mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        CollegeStudent verifyStudent = studentDao.findByEmailAddress("dill@us123");
        assertNotNull(verifyStudent);

    }

    @Test
    void deleteStudentHttpRequest() throws Exception {
        Integer id = 1;
        assertTrue(studentDao.findById(id).isPresent());
        mockMvc.perform(delete("/student/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        Optional<CollegeStudent> studentById = studentDao.findById(id);
        assertFalse(studentById.isPresent());
    }

    @Test
    void deleteStudentHttpRequestErrorPage() throws Exception {
        Integer id = 0;
        assertFalse(studentDao.findById(id).isPresent());
        mockMvc.perform(delete("/student/{id}", id))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    void studentInformationHttpRequest() throws Exception {
        Integer id = 1;
        Optional<CollegeStudent> student = studentDao.findById(id);
        assertTrue(student.isPresent());
        mockMvc.perform(get("/studentInformation/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.firstname", is("madhav")))
                .andExpect(jsonPath("$.lastname", is("anupoju")))
                .andExpect(jsonPath("$.emailAddress", is("madhav@123")));
    }

    @Test
    void studentInformationHttpRequestEmptyResponse() throws Exception {
        Integer id = 0;
        Optional<CollegeStudent> student = studentDao.findById(id);
        assertFalse(student.isPresent());
        mockMvc.perform(get("/studentInformation/{id}", id))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    void createAValidGradeHttpRequest() throws Exception {
        mockMvc.perform(post("/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("grade", "85.00")
                        .param("gradeType", "math")
                        .param("studentId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstname", is("madhav")))
                .andExpect(jsonPath("$.lastname", is("anupoju")))
                .andExpect(jsonPath("$.emailAddress", is("madhav@123")))
                .andExpect(jsonPath("$.studentGrades.mathGradeResults", hasSize(2)));
    }

    @Test
    void createAValidGradeHttpRequestStudentDoesNotExitEmptyResonse() throws Exception {
        mockMvc.perform(post("/grades").contentType(MediaType.APPLICATION_JSON)
                        .param("grade", "85.0")
                        .param("gradeType", "math")
                        .param("studentId", "0")
                ).andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    void createAnInvalidGradeHttpRequestGradeTypeDoesNotExistEmptyReponse() throws Exception {
        mockMvc.perform(post("/grades").contentType(MediaType.APPLICATION_JSON)
                        .param("grade", "85.5")
                        .param("gradeType", "literature")
                        .param("studentId", "1")
                ).andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    void deleteAValidGradeHttpRequest() throws Exception{
        Integer id = 1;
        Optional<MathGrade> mathGrade = mathGradeDao.findById(id);
        assertTrue(mathGrade.isPresent());
        mockMvc.perform(delete("/grades/{id}/{gradeType}",1,"math"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstname",is("madhav")))
                .andExpect(jsonPath("$.lastname",is("anupoju")))
                .andExpect(jsonPath("$.emailAddress",is("madhav@123")))
                .andExpect(jsonPath("$.studentGrades.mathGradeResults", hasSize(0)));
    }

    @Test
    void deleteAValidGradeHttpRequestStudentIdDoesNotExistEmptyResponse()throws Exception{
        Integer id=2;
        mockMvc.perform(delete("/grades/{id}/{gradeType}",id,"math"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status",is(404)))
                .andExpect(jsonPath("$.message",is("Student or Grade was not found")));
    }

    @Test
    void deleteAnInvalidGradeHttpRequest() throws Exception {
        mockMvc.perform(delete("/grades/{id}/{gradeType}",1,"leterature"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status",is(404)))
                .andExpect(jsonPath("$.message",is("Student or Grade was not found")));
    }

    @AfterEach
    public void setupAfterTransaction() {
        jdbc.execute(sqlDeleteStudent);
        jdbc.execute(sqlDeleteMathGrade);
        jdbc.execute(sqlDeleteScienceGrade);
        jdbc.execute(sqlDeleteHistoryGrade);
    }


}