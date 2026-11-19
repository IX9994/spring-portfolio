package jp.ac.sanko.entity;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
@Entity
@Table(name = "TASK")
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_seq_gen")
	@SequenceGenerator(name = "task_seq_gen", sequenceName = "TASK_SEQ", allocationSize = 1)
	@Column(name = "ID")
	private Integer id;

	@Column(name = "TITLE", nullable = false, length = 100)
	private String title;

	@Column(name = "DESCRIPTION", length = 500)
	private String description;

	// 0:未完了 1:完了
	@Column(name = "COMPLETED", nullable = false)
	private Integer completed = 0;

	// 追加日（登録時に自動設定、更新時は変更しない）
	@Column(name = "CREATED_DATE", nullable = false, updatable = false)
	private LocalDate createdDate = LocalDate.now();

	// 期限
	@Column(name = "DUE_DATE")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate dueDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "USER_ID", nullable = false, updatable = false)
	private User user;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Task() {
	}

	public Task(String title, String description, Integer completed) {
		this.title = title;
		this.description = description;
		this.completed = completed;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getCompleted() {
		return completed;
	}

	public void setCompleted(Integer completed) {
		this.completed = completed;
	}

	public String getCompletedLabel() {
		return (completed != null && completed == 1) ? "完了" : "未完了";
	}

	public boolean isDone() {
		return completed != null && completed == 1;
	}

	public LocalDate getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(LocalDate createdDate) {
		this.createdDate = createdDate;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	// 未完了で、期限が今日より前なら期限切れ
	public boolean isOverdue() {
		return !isDone() && dueDate != null && dueDate.isBefore(LocalDate.now());
	}
}
