package robocorp.dap;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import robocorp.lsp.psi.LSPPsiAstElement;
import robocorp.robot.intellij.RobotElementType;
import robocorp.robot.intellij.RobotPsiFile;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class RobotRunnerLineMarkerProvider implements LineMarkerProvider {
    private static final String ROBOT_FILE_SUFFIX = ".robot";
    private static final List<String> ROBOT_TEST_CASE_NAME = Arrays.asList("test case", "test cases", "task", "tasks");


    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        // is root or isn't .robot
        if (!(file instanceof RobotPsiFile) || !file.getName().endsWith(ROBOT_FILE_SUFFIX)
                || !(element instanceof LSPPsiAstElement)) {
            return null;
        }
        String text = element.getText().strip();
        // text is empty or is leaf node
        if (text.length() == 0 || element.getFirstChild() == null) {
            return null;
        }
        if (isTestCaseElement(element)) {
            return new RobotRunLineMarkerInfo(element.getFirstChild(), new RobotRunAction());
        }
        // is keyword
        if (isKeywordScope(element)) {
            return null;
        }
        PsiElement prev = element.getPrevSibling();
        // isn't test case
        if (prev == null || !RobotElementType.NEW_LINE.equals(prev.getNode().getElementType())) {
            return null;
        }
        // test case
        for (; prev!= null; prev = prev.getPrevSibling()) {
            if (isTestCaseElement(prev)) {
                PsiElement child = element.getFirstChild();
                return new RobotRunLineMarkerInfo(child, new RobotRunAction(getCaseName(child)));
            }
        }
        return null;
    }
    private boolean isTestCaseElement(@NotNull PsiElement element) {
        IElementType elementType = element.getNode().getElementType();
        if (elementType != RobotElementType.HEADING) {
            return false;
        }

        String text = element.getText().replace('*', ' ').toLowerCase().trim();
        return ROBOT_TEST_CASE_NAME.contains(text);
    }

    private boolean isKeywordScope(@NotNull PsiElement element) {
        for (PsiElement prev = element; prev != null; prev = prev.getPrevSibling()) {
            IElementType elementType = prev.getNode().getElementType();
            String text = prev.getText().replace('*', ' ').toLowerCase().trim();
            if (elementType == RobotElementType.HEADING) {
                return "keywords".equals(text);
            }
        }
        return false;
    }

    private static String getCaseName(PsiElement element) {
        StringBuilder sb = new StringBuilder();
        for (element = element.getParent(); element != null && !RobotElementType.NEW_LINE.equals(element.getNode().getElementType()); element = element.getNextSibling()) {
            sb.append(element.getText());
        }
        return sb.toString();
    }

    static class RobotRunLineMarkerInfo extends MergeableLineMarkerInfo<PsiElement> {
        private final AnAction anAction;

        public RobotRunLineMarkerInfo(@NotNull PsiElement element, AnAction anAction) {
            super(element, element.getTextRange(), AllIcons.RunConfigurations.TestState.Run,
                    (e) -> {
                        if (anAction instanceof  RobotRunAction) {
                            String name = ((RobotRunAction) anAction).getName();
                            if (null == name) {
                                return "run all case";
                            } else {
                                return "run " + name;
                            }
                        }
                        return "run";
                    },
                    null, GutterIconRenderer.Alignment.CENTER, () -> RobotRunnerLineMarkerProvider.getCaseName(element));
            this.anAction = anAction;
        }

        @Override
        public GutterIconRenderer createGutterRenderer() {
            return new LineMarkerGutterIconRenderer<>(this) {
                @Override
                public AnAction getClickAction() {
                    return anAction;
                }
            };
        }

        @Override
        public boolean canMergeWith(@NotNull MergeableLineMarkerInfo<?> info) {
            return true;
        }

        @Override
        public Icon getCommonIcon(@NotNull List<? extends MergeableLineMarkerInfo<?>> infos) {
            return AllIcons.RunConfigurations.TestState.Run;
        }
    }
}
