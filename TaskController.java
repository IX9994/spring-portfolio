package jp.ac.sanko.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpSession;
import jp.ac.sanko.entity.Task;
import jp.ac.sanko.repository.TaskRepository;
import jp.ac.sanko.repository.UserRepository;

@Controller
public class TaskController {

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private UserRepository userRepository;

	private Integer loginUserId(HttpSession session) {
		return (Integer) session.getAttribute("ユーザーID");
	}

	// ログイン済みかどうかをセッションから判定する共通処理
	private boolean isLoggedIn(HttpSession session) {
		return session.getAttribute("ユーザーID") != null;
	}

	// タスクの入力内容を確認する共通処理
	private String validateTask(Task task) {

		if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
			return "タスク名を入力してください。";
		}

		if (task.getTitle().trim().length() > 100) {
			return "タスク名は100文字以内で入力してください。";
		}

		if (task.getDescription() != null &&
				task.getDescription().length() > 500) {
			return "詳細は500文字以内で入力してください。";
		}

		if (task.getCompleted() != null &&
				task.getCompleted() != 0 &&
				task.getCompleted() != 1) {
			return "完了状態の値が正しくありません。";
		}

		return null;
	}

	// 検索条件を維持したまま /index へリダイレクトするURLを組み立てる
	private String redirectToIndex(String keyword, Integer completed, String due, String sort) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/index");
		if (keyword != null && !keyword.isEmpty()) {
			builder.queryParam("keyword", keyword);
		}
		if (completed != null) {
			builder.queryParam("completed", completed);
		}
		if (due != null && !due.isEmpty()) {
			builder.queryParam("due", due);
		}
		if (sort != null && !sort.isEmpty() && !"id".equals(sort)) {
			builder.queryParam("sort", sort);
		}
		return "redirect:" + builder.toUriString();
	}

	/**
	 * タスク一覧画面（全件検索／キーワード検索／完了状態での絞り込み）
	 */
	@RequestMapping(path = "/index", method = RequestMethod.GET)
	public String taskManagement(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Integer completed,
			@RequestParam(required = false) String due,
			@RequestParam(defaultValue = "id") String sort,
			HttpSession session,
			Model model) {

		if (!isLoggedIn(session)) {
			return "redirect:/login";
		}

		Integer userId = loginUserId(session);

		// 条件がないときは、nullではなく「何も絞らない値」を渡す
		String kw = (keyword == null) ? "" : keyword.trim();
		int comp = (completed == null) ? -1 : completed;

		// 期限の絞り込み（useDue=1のとき from〜to の範囲）
		LocalDate today = LocalDate.now();
		int useDue = 0;
		LocalDate from = today;
		LocalDate to = today;
		if ("overdue".equals(due)) { // 期限切れ：昔〜昨日
			useDue = 1;
			from = LocalDate.of(1900, 1, 1);
			to = today.minusDays(1);
		} else if ("today".equals(due)) { // 今日が期限
			useDue = 1;
		} else if ("week".equals(due)) { // 今日から7日以内
			useDue = 1;
			to = today.plusDays(7);
		}

		// 並び順：期限が近い順（期限なしは最後）／登録順
		Sort order = "due".equals(sort)
				? Sort.by(Sort.Order.asc("dueDate").nullsLast(), Sort.Order.asc("id"))
				: Sort.by("id");

		List<Task> tasks = taskRepository.search(userId, kw, comp, useDue, from, to, order);

		// 進捗表示も自分のタスクだけで集計
		List<Task> allTasks = taskRepository.findByUser_IdOrderByIdAsc(userId);
		long totalCount = allTasks.size();
		long doneCount = allTasks.stream().filter(Task::isDone).count();

		// 期限の件数（JPQLのCOUNTで取得）
		long overdueCount = taskRepository.countOverdue(userId, today);
		long dueTodayCount = taskRepository.countDueToday(userId, today);

		model.addAttribute("tasks", tasks);
		model.addAttribute("keyword", keyword);
		model.addAttribute("completed", completed);
		model.addAttribute("due", due);
		model.addAttribute("sort", sort);
		model.addAttribute("totalCount", totalCount);
		model.addAttribute("doneCount", doneCount);
		model.addAttribute("overdueCount", overdueCount);
		model.addAttribute("dueTodayCount", dueTodayCount);
		return "/index";
	}

	/**
	 * タスク詳細画面（主キー検索）
	 */
	@RequestMapping(path = "/task/{id}", method = RequestMethod.GET)
	public String taskDetail(@PathVariable Integer id, HttpSession session, Model model) {
		if (!isLoggedIn(session)) {
			return "redirect:/login";
		}
		Task task = taskRepository.findByIdAndUser_Id(id, loginUserId(session)).orElse(null);
		model.addAttribute("task", task);
		return "/taskDetail";
	}

	/**
	 * タスク追加画面の表示
	 */
	@RequestMapping(path = "/task/add", method = RequestMethod.GET)
	public String addTaskForm(HttpSession session, Model model) {
		if (!isLoggedIn(session)) {
			return "redirect:/login";
		}
		model.addAttribute("task", new Task());
		return "/taskAdd";
	}

	/**
	 * タスク追加の実行
	 */
	@RequestMapping(path = "/task/add", method = RequestMethod.POST)
	public String addTask(@ModelAttribute Task task, HttpSession session, Model model) {
		if (!isLoggedIn(session)) {
			return "redirect:/login";
		}
		String errorMessage = validateTask(task);

		if (errorMessage != null) {
			model.addAttribute("errorMessage", errorMessage);
			model.addAttribute("task", task);
			return "/taskAdd";
		}
		task.setId(null);
		task.setTitle(task.getTitle().trim());
		task.setCompleted(0);
		task.setCreatedDate(LocalDate.now());

		task.setUser(userRepository.getReferenceById(loginUserId(session)));
		taskRepository.save(task);

		return "redirect:/index";
	}

	/**
	 * タスク編集画面の表示
	 */
	@RequestMapping(path = "/task/{id}/edit", method = RequestMethod.GET)
	public String editTaskForm(@PathVariable Integer id, HttpSession session, Model model) {
		if (!isLoggedIn(session)) {
			return "redirect:/login";
		}
		Task task = taskRepository.findByIdAndUser_Id(id, loginUserId(session)).orElse(null);
		if (task == null) {
			return "redirect:/index";
		}
		model.addAttribute("task", task);
		return "/taskEdit";
	}

	/**
	 * タスク編集の実行
	 */
	@RequestMapping(path = "/task/{id}/edit", method = RequestMethod.POST)
	public String editTask(
			@PathVariable Integer id,
			@ModelAttribute Task task,
			HttpSession session,
			Model model) {

		if (!isLoggedIn(session)) {
			return "redirect:/login";
		}

		// ログイン中のユーザーが所有するタスクか確認
		Task existing = taskRepository
				.findByIdAndUser_Id(id, loginUserId(session))
				.orElse(null);

		if (existing == null) {
			return "redirect:/index";
		}

		// 入力値の検証
		String errorMessage = validateTask(task);

		if (errorMessage != null) {
			model.addAttribute("errorMessage", errorMessage);
			task.setId(id);
			model.addAttribute("task", task);
			return "/taskEdit";
		}

		// 検証に成功した値だけを既存のタスクに反映
		existing.setTitle(task.getTitle().trim());
		existing.setDescription(task.getDescription());
		existing.setDueDate(task.getDueDate());

		if (task.getCompleted() != null) {
			existing.setCompleted(task.getCompleted());
		}

		taskRepository.save(existing);

		return "redirect:/index";
	}

	/**
	 * 達成／未達成の切り替え
	 */
	@RequestMapping(path = "/task/{id}/toggle", method = RequestMethod.POST)
	public String toggleTask(
			@PathVariable Integer id,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Integer completed,
			@RequestParam(required = false) String due,
			@RequestParam(required = false) String sort,
			HttpSession session) {

		if (!isLoggedIn(session)) {
			return "redirect:/login";
		}
		Task task = taskRepository.findByIdAndUser_Id(id, loginUserId(session)).orElse(null);
		if (task != null) {
			task.setCompleted(task.isDone() ? 0 : 1);
			taskRepository.save(task);
		}
		return redirectToIndex(keyword, completed, due, sort);
	}

	/**
	 * タスク削除
	 */
	@RequestMapping(path = "/task/{id}/delete", method = RequestMethod.POST)
	public String deleteTask(
			@PathVariable Integer id,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Integer completed,
			@RequestParam(required = false) String due,
			@RequestParam(required = false) String sort,
			HttpSession session) {

		if (!isLoggedIn(session)) {
			return "redirect:/login";
		}
		taskRepository.findByIdAndUser_Id(id, loginUserId(session)).ifPresent(t -> taskRepository.delete(t));
		return redirectToIndex(keyword, completed, due, sort);
	}

}
