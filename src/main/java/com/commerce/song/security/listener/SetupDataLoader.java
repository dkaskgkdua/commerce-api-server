package com.commerce.song.security.listener;

import com.commerce.song.domain.entity.Account;
import com.commerce.song.domain.entity.Resources;
import com.commerce.song.domain.entity.Role;
import com.commerce.song.repository.AccountRepository;
import com.commerce.song.repository.ResourcesRepository;
import com.commerce.song.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


@Component
@RequiredArgsConstructor
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent> {
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final ResourcesRepository resourcesRepository;
    private final PasswordEncoder passwordEncoder;
    private boolean alreadySetup = false;

    private static AtomicInteger count = new AtomicInteger(0);

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if(alreadySetup) {
            return;
        }
        setupSecurityResources();
    }

    private void setupSecurityResources() {
        Role adminRole = createRoleIfNotFound("ROLE_ADMIN", "관리자");
        Role managerRole = createRoleIfNotFound("ROLE_MANAGER", "매니저");
        Role userRole = createRoleIfNotFound("ROLE_USER", "사용자");

        Set<Role> adminRoleSet = new HashSet<>();
        adminRoleSet.add(adminRole);
        createResourceIfNotFound("/api/v1/admin/test", "", adminRoleSet, "url");
        adminRoleSet.add(managerRole);
        adminRoleSet.add(userRole);


        Set<Role> managerRoleSet = new HashSet<>();
        managerRoleSet.add(managerRole);
        managerRoleSet.add(userRole);

        Set<Role> userRoleSet = new HashSet<>();
        userRoleSet.add(userRole);

        Account adminAccount = createAccountIfNotFound("admin", "1111", "admin@gmail.com", "19941126", adminRoleSet, "01011111111");
        Account managerAccount = createAccountIfNotFound("manager", "1111", "manager@gmail.com", "19931111", managerRoleSet, "01011111111");
        Account userAccount = createAccountIfNotFound("user", "1111", "user@gmail.com", "19921122", userRoleSet, "01011111111");


    }

    @Transactional
    public Resources createResourceIfNotFound(String resourceName, String httpMethod, Set<Role> roleSet, String resourceType) {
        Resources resources = resourcesRepository.findByResourceNameAndHttpMethod(resourceName, httpMethod);

        if (resources == null) {
            resources = Resources.builder()
                    .resourceName(resourceName)
                    .roleSet(roleSet)
                    .httpMethod(httpMethod)
                    .resourceType(resourceType)
                    .orderNum(count.incrementAndGet())
                    .build();
        }
        return resourcesRepository.save(resources);
    }

    @Transactional
    public Account createAccountIfNotFound(String userName, String password, String email, String birth, Set<Role> role, String phone) {
        Account account = accountRepository.findByEmail(email);

        if(account == null) {
            account = Account.builder()
                    .username(userName)
                    .email(email)
                    .birth(birth)
                    .phone(phone)
                    .password(passwordEncoder.encode(password))
                    .userRoles(role)
                    .build();
        }
        return accountRepository.save(account);
    }

    @Transactional
    public Role createRoleIfNotFound(String roleName, String roleDesc) {
        Role role = roleRepository.findByRoleName(roleName);

        if(role == null) {
            role = Role.builder()
                    .roleName(roleName)
                    .roleDesc(roleDesc)
                    .build();
        }
        return roleRepository.save(role);
    }
}
