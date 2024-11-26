module testt {
	requires javafx.controls;
	requires java.desktop;
	requires javafx.graphics;
	requires opencv;
	requires javafx.media;
	requires pdfbox.app;
	requires json;
	
	
	opens application to javafx.graphics, javafx.fxml;
}
