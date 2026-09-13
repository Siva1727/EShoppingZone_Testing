package com.eshoppingzone.cart.service;

import com.eshoppingzone.cart.client.ProductClient;
import com.eshoppingzone.cart.dto.AddToCartRequest;
import com.eshoppingzone.cart.dto.ApiResponse;
import com.eshoppingzone.cart.dto.CartDto;
import com.eshoppingzone.cart.dto.ProductSnapshotDto;
import com.eshoppingzone.cart.entity.Cart;
import com.eshoppingzone.cart.entity.CartItem;
import com.eshoppingzone.cart.exception.ResourceNotFoundException;
import com.eshoppingzone.cart.repository.CartItemRepository;
import com.eshoppingzone.cart.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           ProductClient productClient) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productClient = productClient;
    }

    private Cart getOrCreateCart(Long customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setCustomerId(customerId);
                    return cartRepository.save(cart);
                });
    }

    @Override
    @Transactional
    public CartDto getMyCart(Long customerId) {
        Cart cart = getOrCreateCart(customerId);
        return CartDto.fromEntity(cart);
    }

    @Override
    @Transactional
    public CartDto addToCart(Long customerId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(customerId);

        // Fetch product snapshot from Product Service
        String productName = "Product #" + request.getProductId();
        BigDecimal unitPrice = BigDecimal.valueOf(10.00);

        try {
            ApiResponse<ProductSnapshotDto> productResponse = productClient.getProductById(request.getProductId());
            if (productResponse != null && productResponse.isSuccess() && productResponse.getData() != null) {
                productName = productResponse.getData().getName();
                unitPrice = productResponse.getData().getPrice();
            }
        } catch (Exception e) {
            log.warn("Could not retrieve product from ProductClient: {}", e.getMessage());
        }

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartAndProductId(cart, request.getProductId());

        if (existingItemOpt.isPresent()) {
            CartItem existing = existingItemOpt.get();
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            existing.setUnitPrice(unitPrice);
            existing.setProductName(productName);
            cartItemRepository.save(existing);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(request.getProductId());
            newItem.setProductName(productName);
            newItem.setUnitPrice(unitPrice);
            newItem.setQuantity(request.getQuantity());
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        Cart updatedCart = cartRepository.save(cart);
        return CartDto.fromEntity(updatedCart);
    }

    @Override
    @Transactional
    public CartDto updateCartItemQuantity(Long customerId, Long itemId, Integer quantity) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = cartItemRepository.findByIdAndCart(itemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        Cart updatedCart = cartRepository.save(cart);
        return CartDto.fromEntity(updatedCart);
    }

    @Override
    @Transactional
    public CartDto removeCartItem(Long customerId, Long itemId) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = cartItemRepository.findByIdAndCart(itemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        Cart updatedCart = cartRepository.save(cart);
        return CartDto.fromEntity(updatedCart);
    }

    @Override
    @Transactional
    public void clearCart(Long customerId) {
        Cart cart = getOrCreateCart(customerId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartDto getCartByCustomerId(Long customerId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for customer: " + customerId));
        return CartDto.fromEntity(cart);
    }
}
