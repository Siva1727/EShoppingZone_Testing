package com.eshoppingzone.cart.service;

import com.eshoppingzone.cart.client.ProductClient;
import com.eshoppingzone.cart.dto.AddToCartRequest;
import com.eshoppingzone.cart.dto.ApiResponse;
import com.eshoppingzone.cart.dto.CartDto;
import com.eshoppingzone.cart.dto.ProductSnapshotDto;
import com.eshoppingzone.cart.entity.Cart;
import com.eshoppingzone.cart.entity.CartItem;
import com.eshoppingzone.cart.repository.CartItemRepository;
import com.eshoppingzone.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart sampleCart;

    @BeforeEach
    void setUp() {
        sampleCart = new Cart(1L, 100L);
        sampleCart.setItems(new ArrayList<>());
    }

    @Test
    void testGetMyCartReturnsExistingOrCreates() {
        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(sampleCart));

        CartDto cartDto = cartService.getMyCart(100L);

        assertNotNull(cartDto);
        assertEquals(100L, cartDto.getCustomerId());
        assertEquals(0, cartDto.getTotalItems());
    }

    @Test
    void testAddToCartNewItem() {
        AddToCartRequest request = new AddToCartRequest(1L, 2);
        ProductSnapshotDto productSnapshot = new ProductSnapshotDto(1L, 2L, "Smartphone", new BigDecimal("500.00"), "ACTIVE");

        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(sampleCart));
        when(productClient.getProductById(1L)).thenReturn(ApiResponse.success(productSnapshot));
        when(cartItemRepository.findByCartAndProductId(sampleCart, 1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);

        CartDto result = cartService.addToCart(100L, request);

        assertNotNull(result);
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
        verify(cartRepository, times(1)).save(sampleCart);
    }

    @Test
    void testClearCart() {
        CartItem item = new CartItem(1L, sampleCart, 1L, "Item", new BigDecimal("10.00"), 1);
        sampleCart.getItems().add(item);

        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(sampleCart));

        cartService.clearCart(100L);

        assertTrue(sampleCart.getItems().isEmpty());
        verify(cartRepository, times(1)).save(sampleCart);
    }
}
