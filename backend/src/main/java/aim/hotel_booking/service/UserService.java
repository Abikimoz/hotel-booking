package aim.hotel_booking.service;

import aim.hotel_booking.entity.UserEntity;
import aim.hotel_booking.mapper.UserMapper;
import aim.hotel_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.UserCreateDto;
import org.openapitools.model.UserDto;
import org.openapitools.model.UserFilters;
import org.openapitools.model.UsersListUsers200Response;
import org.openapitools.model.HotelsListHotels200ResponsePagination;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;

import java.net.URI;

import static aim.hotel_booking.repository.specification.UserSpecifications.*;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;

    public ResponseEntity<UserDto> createUser(UserCreateDto dto) {
        UserEntity entity = mapper.toEntityWithPassword(dto, encoder);
        UserEntity savedEntity = repository.save(entity);
        UserDto responseDto = mapper.toDto(savedEntity);
        return ResponseEntity
                .created(URI.create("/users/" + savedEntity.getId()))
                .body(responseDto);
    }

    public ResponseEntity<UsersListUsers200Response> getUsers(
            String name,
            String email,
            String sortBy,
            Sort.Direction sortOrderDirection,
            int page,
            int perPage
    ) {
        // Создаем спецификации для фильтрации
        Specification<UserEntity> spec = Specification.where(null);

        if (name != null && !name.isEmpty()) {
            spec = spec.and(hasNameLike(name));
        }

        if (email != null && !email.isEmpty()) {
            spec = spec.and(hasEmailLike(email));
        }

        // Создаем объект пагинации и сортировки
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by(sortOrderDirection, sortBy));

        // Получаем страницу с пользователями
        Page<UserEntity> userPage = repository.findAll(spec, pageable);

        // Создаем объекты для ответа
        UserFilters filters = new UserFilters()
                .name(name)
                .email(email);

        HotelsListHotels200ResponsePagination pagination = new HotelsListHotels200ResponsePagination()
                .page(page)
                .perPage(perPage)
                .totalPages(userPage.getTotalPages())
                .total((int) userPage.getTotalElements());

        // Преобразуем Sort.Direction в SortOrderEnum
        UsersListUsers200Response.SortOrderEnum sortOrder =
                UsersListUsers200Response.SortOrderEnum.fromValue(sortOrderDirection.toString());

        // Преобразуем строку sortBy в SortByEnum
        UsersListUsers200Response.SortByEnum sortByEnum;
        try {
            sortByEnum = UsersListUsers200Response.SortByEnum.fromValue(sortBy);
        } catch (IllegalArgumentException e) {
            sortByEnum = UsersListUsers200Response.SortByEnum.NAME;
        }

        // Создаем ответ
        UsersListUsers200Response response = new UsersListUsers200Response(
                mapper.toDtoList(userPage.getContent()),
                pagination,
                filters,
                sortByEnum,
                sortOrder
        );

        return ResponseEntity.ok(response);
    }
}
