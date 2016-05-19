package yuku.kpri.model;

public enum VerseKind {
	NORMAL(0),
	REFRAIN(1),
	TEXT(2),
	;
	
	public final int value;

	private VerseKind(int value) {
		this.value = value;
	}
}
