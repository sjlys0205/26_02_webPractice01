package com.example.ksl.wsd2025.api.v1;

import com.example.ksl.wsd2025.api.dto.ItemDto;
import com.example.ksl.wsd2025.api.exception.ApiException;
import com.example.ksl.wsd2025.api.request.ItemBulkCreateRequest;
import com.example.ksl.wsd2025.api.request.ItemCreateRequest;
import com.example.ksl.wsd2025.api.request.ItemPriceUpdateRequest;
import com.example.ksl.wsd2025.api.request.ItemUpdateRequest;
import com.example.ksl.wsd2025.api.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    // 메모리용 임시 DB (예제용) - 동시 요청에 안전한 자료구조 사용
    private final Map<Long, ItemDto> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    // ===================== POST =====================

    // POST 1) 단건 생성 → 201 Created (+ Location 헤더)
    //   400: 검증 실패, 409: 같은 이름의 아이템이 이미 존재
    @PostMapping
    public ResponseEntity<ApiResponse<ItemDto>> createItem(@Valid @RequestBody ItemCreateRequest request) {
        ItemDto item;
        synchronized (store) {
            checkDuplicateName(request.getName(), null);
            item = saveNewItem(request);
        }
        return ResponseEntity.created(URI.create("/api/v1/items/" + item.getId()))
                .body(ApiResponse.created(item, "Item created"));
    }

    // POST 2) 여러 개 한 번에 생성 → 201 Created
    //   400: 검증 실패, 409: 이름 중복 (하나라도 중복이면 전체 실패)
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<ItemDto>>> createItems(@Valid @RequestBody ItemBulkCreateRequest request) {
        List<ItemDto> created = new ArrayList<>();
        synchronized (store) {
            Set<String> names = new HashSet<>();
            for (ItemCreateRequest itemRequest : request.getItems()) {
                if (!names.add(itemRequest.getName())) {
                    throw new ApiException(HttpStatus.CONFLICT,
                            "Duplicate name in request: " + itemRequest.getName());
                }
                checkDuplicateName(itemRequest.getName(), null);
            }
            for (ItemCreateRequest itemRequest : request.getItems()) {
                created.add(saveNewItem(itemRequest));
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, created.size() + " items created"));
    }

    // ===================== GET =====================

    // GET 1) 목록 조회 (검색 + 페이지네이션) → 200 OK
    //   /api/v1/items?keyword=abc&page=0&size=10  (page는 0부터 시작)
    //   400: page < 0 또는 size가 1~100 범위를 벗어남
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "page must be 0 or greater");
        }
        if (size < 1 || size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "size must be between 1 and 100");
        }

        List<ItemDto> filtered = store.values().stream()
                .filter(item -> keyword == null || item.getName().contains(keyword))
                .sorted(Comparator.comparing(ItemDto::getId))
                .toList();
        List<ItemDto> content = filtered.stream()
                .skip((long) page * size)
                .limit(size)
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", content);
        data.put("page", page);
        data.put("size", size);
        data.put("totalCount", filtered.size());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // GET 2) 단건 조회 → 200 OK
    //   400: id가 숫자가 아님, 404: 아이템 없음
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemDto>> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(findItem(id)));
    }

    // ===================== PUT =====================

    // PUT 1) 전체 수정 (name, price 모두 필수) → 200 OK
    //   400: 검증 실패, 404: 아이템 없음, 409: 다른 아이템과 이름 중복
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemDto>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemUpdateRequest request
    ) {
        ItemDto item;
        synchronized (store) {
            item = findItem(id);
            checkDuplicateName(request.getName(), id);
            item.setName(request.getName());
            item.setPrice(request.getPrice());
        }
        return ResponseEntity.ok(ApiResponse.success(item, "Item updated"));
    }

    // PUT 2) 가격만 수정 → 200 OK
    //   400: 검증 실패, 404: 아이템 없음
    @PutMapping("/{id}/price")
    public ResponseEntity<ApiResponse<ItemDto>> updateItemPrice(
            @PathVariable Long id,
            @Valid @RequestBody ItemPriceUpdateRequest request
    ) {
        ItemDto item = findItem(id);
        item.setPrice(request.getPrice());
        return ResponseEntity.ok(ApiResponse.success(item, "Item price updated"));
    }

    // ===================== DELETE =====================

    // DELETE 1) 단건 삭제 → 200 OK (삭제된 아이템 반환)
    //   404: 아이템 없음
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemDto>> deleteItem(@PathVariable Long id) {
        ItemDto removed = store.remove(id);
        if (removed == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Item not found: id=" + id);
        }
        return ResponseEntity.ok(ApiResponse.success(removed, "Item deleted"));
    }

    // DELETE 2) 전체 삭제 → 200 OK (삭제된 개수 반환)
    @DeleteMapping
    public ResponseEntity<ApiResponse<Map<String, Integer>>> deleteAllItems() {
        int count;
        synchronized (store) {
            count = store.size();
            store.clear();
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("deletedCount", count), "All items deleted"));
    }

    // ===================== helper =====================

    private ItemDto findItem(Long id) {
        ItemDto item = store.get(id);
        if (item == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Item not found: id=" + id);
        }
        return item;
    }

    // excludeId: 수정 시 자기 자신은 중복 검사에서 제외
    private void checkDuplicateName(String name, Long excludeId) {
        boolean exists = store.values().stream()
                .anyMatch(item -> item.getName().equals(name) && !item.getId().equals(excludeId));
        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT, "Item name already exists: " + name);
        }
    }

    private ItemDto saveNewItem(ItemCreateRequest request) {
        ItemDto item = new ItemDto();
        item.setId(sequence.getAndIncrement());
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        store.put(item.getId(), item);
        return item;
    }
}
