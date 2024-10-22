package com.example.caching;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@SpringBootTest
public class SimpleBookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    public void setUp() {
        // Clear caches before each test
        cacheManager.getCache("books").clear();
    }

    @Test
    public void testCachingBehavior() {
        // The first call should take longer since it triggers simulateSlowService()
        long startTime = System.currentTimeMillis();
        Book book1 = bookRepository.getByIsbn("isbn-1234");
        long firstCallDuration = System.currentTimeMillis() - startTime;
        assertTrue(firstCallDuration >= 3000, "First call should take at least 3 seconds.");

        // The second call should be fast because it comes from the cache
        startTime = System.currentTimeMillis();
        Book book2 = bookRepository.getByIsbn("isbn-1234");
        long secondCallDuration = System.currentTimeMillis() - startTime;
        assertTrue(secondCallDuration < 100, "Second call should be fast (less than 100ms) due to caching.");

        // Assert that both calls returned the same result
        assertEquals(book1, book2, "Books returned from both calls should be the same.");

        // Assert that the book is cached
        Book cachedBook = (Book) cacheManager.getCache("books").get("isbn-1234").get();
        assertNotNull(cachedBook, "Book should be present in the cache.");
        assertEquals("isbn-1234", cachedBook.getIsbn());
        assertEquals("BookTitle_isbn-1234", cachedBook.getTitle());
    }

    @EnableCaching
    @Configuration
    static class TestConfig {

        @Bean
        public SimpleBookRepository bookRepository() {
            return new SimpleBookRepository();
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("books");
        }
    }
}
