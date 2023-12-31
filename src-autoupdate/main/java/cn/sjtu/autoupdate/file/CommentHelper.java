package cn.sjtu.autoupdate.file;

import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.code.CtJavaDocTag;
import spoon.reflect.visitor.PrinterHelper;

import static spoon.reflect.visitor.DefaultJavaPrettyPrinter.JAVADOC_START;
import static spoon.reflect.visitor.DefaultJavaPrettyPrinter.INLINE_COMMENT_START;
import static spoon.reflect.visitor.DefaultJavaPrettyPrinter.BLOCK_COMMENT_START;
import static spoon.reflect.visitor.DefaultJavaPrettyPrinter.BLOCK_COMMENT_END;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static spoon.reflect.visitor.DefaultJavaPrettyPrinter.COMMENT_STAR;

/**
 * Computes source code representation of the Comment literal
 */
class CommentHelper {

	/**
	 * RegExp which matches all possible line separators
	 */
	private static final Pattern LINE_SEPARATORS_RE = Pattern.compile("\\n\\r|\\n|\\r");

	private CommentHelper() {
	}

	static void printComment(PrinterHelper printer, CtComment comment) {
		List<CtJavaDocTag> tags = null;
		if (comment instanceof CtJavaDoc) {
			tags = ((CtJavaDoc) comment).getTags();
		}
		printComment(printer, comment.getCommentType(), comment.getContent(), tags);
	}

	static void printComment(PrinterHelper printer, CtComment.CommentType commentType, String content, Collection<CtJavaDocTag> javaDocTags) {
		switch (commentType) {
		case FILE:
			printer.write(JAVADOC_START).writeln();
			break;
		case JAVADOC:
			printer.write(JAVADOC_START).writeln();
			break;
		case INLINE:
			printer.write(INLINE_COMMENT_START);
			break;
		case BLOCK:
			printer.write(BLOCK_COMMENT_START);
			break;
		}
		switch (commentType) {
			case INLINE:
				printer.write(content);
				break;
			default:
				String[] lines = LINE_SEPARATORS_RE.split(content);
				for (int i = 0; i < lines.length; i++) {
					String com = lines[i];
					if (commentType == CtComment.CommentType.BLOCK) {
						printer.write(com);
						if (lines.length > 1) {
							printer.writeln();
						}
					} else {
						if (com.length() > 0) {
							printer.write(COMMENT_STAR + com).writeln();
						} else {
							printer.write(" *" /* no trailing space */ + com).writeln();
						}
					}
				}
				if (javaDocTags != null && javaDocTags.isEmpty() == false) {
					printer.write(" *").writeln();
					for (CtJavaDocTag docTag : javaDocTags) {
						printJavaDocTag(printer, docTag);
					}
				}
				break;
		}
		switch (commentType) {
			case BLOCK:
				printer.write(BLOCK_COMMENT_END);
				break;
			case FILE:
				printer.write(BLOCK_COMMENT_END);
				break;
			case JAVADOC:
				printer.write(BLOCK_COMMENT_END);
				break;
		}
	}

	static void printJavaDocTag(PrinterHelper printer, CtJavaDocTag docTag) {
		printer.write(COMMENT_STAR);
		printer.write(CtJavaDocTag.JAVADOC_TAG_PREFIX);
		printer.write(docTag.getType().name().toLowerCase());
		printer.write(" ");
		if (docTag.getType().hasParam()) {
			printer.write(docTag.getParam()).writeln();
		}

		String[] tagLines = LINE_SEPARATORS_RE.split(docTag.getContent());
		for (int i = 0; i < tagLines.length; i++) {
			String com = tagLines[i];
			if (i > 0 || docTag.getType().hasParam()) {
				printer.write(COMMENT_STAR);
			}
			if (docTag.getType().hasParam()) {
				printer.write("\t\t");
			}
			printer.write(com.trim()).writeln();
		}
	}
}
