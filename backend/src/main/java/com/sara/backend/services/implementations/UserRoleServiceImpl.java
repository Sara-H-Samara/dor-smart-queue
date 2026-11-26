package com.sara.backend.services.implementations;

import com.sara.backend.dtos.requests.RoleChangeRequest;
import com.sara.backend.dtos.responses.UserPageResponse;
import com.sara.backend.dtos.responses.UserRowResponse;
import com.sara.backend.entities.Role;
import com.sara.backend.entities.TicketStatus;
import com.sara.backend.entities.User;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
import com.sara.backend.services.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserRoleServiceImpl implements UserRoleService {

    private static final int LARGEST_PAGE_SIZE = 100;
    private static final int LONGEST_SEARCH = 100;
    private static final Sort NAME_THEN_ID_ORDER = Sort.by("fullName").and(Sort.by("id"));

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Override
    public UserPageResponse listUsers(String search, int page, int size, String callerEmail) {
        rejectInvalidPaging(page, size);
        String trimmedSearch = search.trim();
        rejectTooLongSearch(trimmedSearch);

        PageRequest pageRequest = PageRequest.of(page, size, NAME_THEN_ID_ORDER);
        Page<User> userPage = trimmedSearch.isEmpty()
                ? userRepository.findAll(pageRequest)
                : userRepository.findByNameOrEmailContaining(escapeLikeWildcards(trimmedSearch), pageRequest);
        return UserPageResponse.from(userPage, callerEmail);
    }

    @Override
    @Transactional
    public UserRowResponse changeRole(Long userId, RoleChangeRequest request, String callerEmail) {
        List<User> lockedAdmins = userRepository.findByRoleWithWriteLock(Role.ADMIN);
        rejectCallerWhoIsNoLongerAdmin(lockedAdmins, callerEmail);
        User user = findUserWithWriteLockOrThrow(userId);
        rejectOwnRoleChange(user, callerEmail);

        Role newRole = Role.valueOf(request.role());
        if (user.getRole() == newRole) {
            return UserRowResponse.from(user, false);
        }
        rejectWhileStudentHasActiveTicket(user);

        user.setRole(newRole);
        return UserRowResponse.from(userRepository.save(user), false);
    }

    private void rejectCallerWhoIsNoLongerAdmin(List<User> lockedAdmins, String callerEmail) {
        boolean callerIsAdmin = lockedAdmins.stream().anyMatch(admin -> admin.getEmail().equals(callerEmail));
        if (!callerIsAdmin) {
            throw new InsufficientAuthenticationException("The caller is no longer an admin.");
        }
    }

    private User findUserWithWriteLockOrThrow(Long userId) {
        return userRepository.findByIdWithWriteLock(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private void rejectOwnRoleChange(User user, String callerEmail) {
        if (user.getEmail().equals(callerEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot change your own role.");
        }
    }

    private void rejectWhileStudentHasActiveTicket(User user) {
        if (user.getRole() == Role.STUDENT && ticketRepository.existsByStudentIdAndTicketDateAndStatusIn(
                user.getId(), LocalDate.now(), TicketStatus.ACTIVE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancel or finish this student's ticket first.");
        }
    }

    private void rejectInvalidPaging(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be 0 or more.");
        }
        if (size < 1 || size > LARGEST_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size must be 1 to 100.");
        }
    }

    private void rejectTooLongSearch(String search) {
        if (search.length() > LONGEST_SEARCH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search must be at most 100 characters.");
        }
    }

    private String escapeLikeWildcards(String search) {
        return search.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
