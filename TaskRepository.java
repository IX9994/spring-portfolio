package jp.ac.sanko.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jp.ac.sanko.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Integer> {

	// 指定ユーザーのタスクをすべて、ID昇順で取得
	List<Task> findByUser_IdOrderByIdAsc(Integer userId);

	// 指定ユーザーのタスクのうち、タイトルにキーワードを含むものをID昇順で取得
	List<Task> findByUser_IdAndTitleContainingIgnoreCaseOrderByIdAsc(Integer userId, String keyword);

	// 指定ユーザーのタスクのうち、完了状態が一致するものをID昇順で取得（0:未完了 1:完了）
	List<Task> findByUser_IdAndCompletedOrderByIdAsc(Integer userId, Integer completed);

	// 両方の条件で絞り込んで、ID昇順で取得
	List<Task> findByUser_IdAndTitleContainingIgnoreCaseAndCompletedOrderByIdAsc(
			Integer userId, String keyword, Integer completed);

	// タスクIDと持ち主のユーザーIDが両方一致するものだけ取得
	// 他人のタスクIDをURLで直接指定されても、見つからない（空）扱いになる
	Optional<Task> findByIdAndUser_Id(Integer id, Integer userId);

	// ログイン中のユーザーのタスクだけに絞る
	@Query("SELECT t FROM Task t WHERE t.user.id = :userId "
			// タイトルにキーワードを含む（大文字・小文字は区別しない）。空文字なら全件
			+ "AND LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			// -1なら状態を問わない。それ以外は、状態が一致するもの
			+ "AND (:completed = -1 OR t.completed = :completed) "
			// 期限で絞るときは、未完了で、期限が from〜to の範囲にあるもの
			// （完了済みのタスクが、期限切れとして出ないようにしている）
			+ "AND (:useDue = 0 OR (t.completed = 0 AND t.dueDate BETWEEN :from AND :to))")
	List<Task> search(@Param("userId") Integer userId,
			@Param("keyword") String keyword,
			@Param("completed") Integer completed,
			@Param("useDue") Integer useDue,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to,
			Sort sort);

	/**
	 * 【期限切れの件数】未完了で、期限が today より前のタスクの数を数える。
	 * COUNT(t) は、条件に合う行の数を返す（戻り値は long）。
	 */
	@Query("SELECT COUNT(t) FROM Task t "
			+ "WHERE t.user.id = :userId AND t.completed = 0 AND t.dueDate < :today")
	long countOverdue(@Param("userId") Integer userId, @Param("today") LocalDate today);

	/**
	 * 【今日が期限の件数】未完了で、期限が today と同じ日のタスクの数を数える。
	 */
	@Query("SELECT COUNT(t) FROM Task t "
			+ "WHERE t.user.id = :userId AND t.completed = 0 AND t.dueDate = :today")
	long countDueToday(@Param("userId") Integer userId, @Param("today") LocalDate today);
}