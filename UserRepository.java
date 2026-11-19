package jp.ac.sanko.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.ac.sanko.entity.User;

public interface UserRepository extends JpaRepository<User, Integer>{
	//emailアドレスを0件1件かで検索
	Optional<User> findByEmail(String email);

}
