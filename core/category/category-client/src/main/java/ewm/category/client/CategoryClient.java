package ewm.category.client;

import ewm.category.dto.CategoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * ВНИМАНИЕ: name = "ewm-main-service" — временно, категории пока живут в main-service
 * (в составе будущей "дополнительной функциональности"). При выделении отдельного
 * сервиса для категорий здесь меняется только name.
 */
@FeignClient(name = "ewm-main-service", path = "/internal/categories")
public interface CategoryClient {

    @GetMapping("/{categoryId}")
    CategoryDto getCategory(@PathVariable("categoryId") long categoryId);

    @GetMapping
    List<CategoryDto> getCategories(@RequestParam("ids") List<Long> ids);
}