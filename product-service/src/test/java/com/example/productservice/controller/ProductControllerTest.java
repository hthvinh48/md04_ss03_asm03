package com.example.productservice.controller;

import com.example.productservice.dto.ProductRequestDTO;
import com.example.productservice.dto.ProductResponseDTO;
import com.example.productservice.exception.GlobalExceptionHandler;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProductController.class, GlobalExceptionHandler.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    @DisplayName("POST /api/v1/products - Hợp lệ trả về 201 Created và ProductResponseDTO")
    void testCreateProductSuccess() throws Exception {
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("Laptop Dell XPS")
                .price(BigDecimal.valueOf(25000000))
                .stockQuantity(10)
                .build();

        ProductResponseDTO response = ProductResponseDTO.builder()
                .id(1L)
                .name("Laptop Dell XPS")
                .price(BigDecimal.valueOf(25000000))
                .stockQuantity(10)
                .build();

        when(productService.createProduct(any(ProductRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Laptop Dell XPS"))
                .andExpect(jsonPath("$.price").value(25000000))
                .andExpect(jsonPath("$.stockQuantity").value(10));
    }

    @Test
    @DisplayName("POST /api/v1/products - Price = -500 phải trả về 400 Bad Request với định dạng ApiResponseError")
    void testCreateProductWithNegativePrice() throws Exception {
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("Chuột Logitech")
                .price(BigDecimal.valueOf(-500))
                .stockQuantity(20)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Price must be greater than 0")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /api/v1/products - Tên để trống (@NotBlank) phải trả về 400 Bad Request với ApiResponseError")
    void testCreateProductWithBlankName() throws Exception {
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("")
                .price(BigDecimal.valueOf(100000))
                .stockQuantity(5)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Product name cannot be empty")));
    }

    @Test
    @DisplayName("POST /api/v1/products - StockQuantity âm (@Min(0)) phải trả về 400 Bad Request với ApiResponseError")
    void testCreateProductWithNegativeStock() throws Exception {
        ProductRequestDTO request = ProductRequestDTO.builder()
                .name("Bàn phím cơ")
                .price(BigDecimal.valueOf(500000))
                .stockQuantity(-1)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Stock quantity cannot be negative")));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Lấy chi tiết sản phẩm thành công")
    void testGetProductByIdFound() throws Exception {
        ProductResponseDTO response = ProductResponseDTO.builder()
                .id(1L)
                .name("Laptop Dell XPS")
                .price(BigDecimal.valueOf(25000000))
                .stockQuantity(10)
                .build();

        when(productService.getProductById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Laptop Dell XPS"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Không thấy sản phẩm trả về 404 ApiResponseError")
    void testGetProductByIdNotFound() throws Exception {
        when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }

    @Test
    @DisplayName("GET /api/v1/products - Lấy danh sách toàn bộ sản phẩm")
    void testGetAllProducts() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(
                ProductResponseDTO.builder().id(1L).name("P1").price(BigDecimal.TEN).stockQuantity(5).build(),
                ProductResponseDTO.builder().id(2L).name("P2").price(BigDecimal.valueOf(20)).stockQuantity(8).build()
        ));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
