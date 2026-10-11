package com.example.customerservice.controller;

import com.example.customerservice.dto.CustomerRequestDTO;
import com.example.customerservice.dto.CustomerResponseDTO;
import com.example.customerservice.dto.LoginRequestDTO;
import com.example.customerservice.exception.BadCredentialsException;
import com.example.customerservice.exception.GlobalExceptionHandler;
import com.example.customerservice.exception.ResourceNotFoundException;
import com.example.customerservice.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {CustomerController.class, GlobalExceptionHandler.class})
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    @Test
    @DisplayName("POST /api/v1/customers/register - Đăng ký thành công trả về 201 và CustomerResponseDTO")
    void testRegisterSuccess() throws Exception {
        CustomerRequestDTO request = CustomerRequestDTO.builder()
                .fullName("Nguyen Van A")
                .email("vana@example.com")
                .password("password123")
                .build();

        CustomerResponseDTO response = CustomerResponseDTO.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .email("vana@example.com")
                .build();

        when(customerService.register(any(CustomerRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.email").value("vana@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - Tìm thấy khách hàng trả về 200")
    void testGetCustomerFound() throws Exception {
        CustomerResponseDTO response = CustomerResponseDTO.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .email("vana@example.com")
                .build();

        when(customerService.getCustomerById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.email").value("vana@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - Không thấy ID trả về 404 chuẩn ApiResponseError")
    void testGetCustomerNotFound() throws Exception {
        when(customerService.getCustomerById(999L))
                .thenThrow(new ResourceNotFoundException("Customer not found with id: 999"));

        mockMvc.perform(get("/api/v1/customers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Customer not found with id: 999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("PUT /api/v1/customers/login - Đúng thông tin trả về 200 và CustomerResponseDTO")
    void testLoginSuccess() throws Exception {
        LoginRequestDTO loginReq = LoginRequestDTO.builder()
                .email("vana@example.com")
                .password("password123")
                .build();

        CustomerResponseDTO response = CustomerResponseDTO.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .email("vana@example.com")
                .build();

        when(customerService.login(any(LoginRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.email").value("vana@example.com"));
    }

    @Test
    @DisplayName("PUT /api/v1/customers/login - Sai thông tin trả về message email or password incorrect")
    void testLoginFailure() throws Exception {
        LoginRequestDTO loginReq = LoginRequestDTO.builder()
                .email("vana@example.com")
                .password("wrongpass")
                .build();

        when(customerService.login(any(LoginRequestDTO.class)))
                .thenThrow(new BadCredentialsException("email or password incorrect"));

        mockMvc.perform(put("/api/v1/customers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("email or password incorrect"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
