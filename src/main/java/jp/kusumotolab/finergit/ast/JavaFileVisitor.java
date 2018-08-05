package jp.kusumotolab.finergit.ast;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import jp.kusumotolab.finergit.ast.token.AND;
import jp.kusumotolab.finergit.ast.token.ANNOTATION;
import jp.kusumotolab.finergit.ast.token.ASSERT;
import jp.kusumotolab.finergit.ast.token.ASSIGN;
import jp.kusumotolab.finergit.ast.token.BLOCKCOMMENT;
import jp.kusumotolab.finergit.ast.token.BREAK;
import jp.kusumotolab.finergit.ast.token.BooleanLiteralFactory;
import jp.kusumotolab.finergit.ast.token.CASE;
import jp.kusumotolab.finergit.ast.token.CATCH;
import jp.kusumotolab.finergit.ast.token.CHARLITERAL;
import jp.kusumotolab.finergit.ast.token.CLASS;
import jp.kusumotolab.finergit.ast.token.CLASSNAME;
import jp.kusumotolab.finergit.ast.token.COLON;
import jp.kusumotolab.finergit.ast.token.COMMA;
import jp.kusumotolab.finergit.ast.token.CONTINUE;
import jp.kusumotolab.finergit.ast.token.DECLAREDMETHODNAME;
import jp.kusumotolab.finergit.ast.token.DEFAULT;
import jp.kusumotolab.finergit.ast.token.DO;
import jp.kusumotolab.finergit.ast.token.DOT;
import jp.kusumotolab.finergit.ast.token.ELSE;
import jp.kusumotolab.finergit.ast.token.EXTENDS;
import jp.kusumotolab.finergit.ast.token.FINALLY;
import jp.kusumotolab.finergit.ast.token.FOR;
import jp.kusumotolab.finergit.ast.token.FinerJavaMethodToken;
import jp.kusumotolab.finergit.ast.token.GREAT;
import jp.kusumotolab.finergit.ast.token.IF;
import jp.kusumotolab.finergit.ast.token.IMPLEMENTS;
import jp.kusumotolab.finergit.ast.token.IMPORT;
import jp.kusumotolab.finergit.ast.token.IMPORTNAME;
import jp.kusumotolab.finergit.ast.token.INSTANCEOF;
import jp.kusumotolab.finergit.ast.token.INVOKEDMETHODNAME;
import jp.kusumotolab.finergit.ast.token.JAVADOCCOMMENT;
import jp.kusumotolab.finergit.ast.token.JavaToken;
import jp.kusumotolab.finergit.ast.token.LABELNAME;
import jp.kusumotolab.finergit.ast.token.LEFTBRACKET;
import jp.kusumotolab.finergit.ast.token.LEFTPAREN;
import jp.kusumotolab.finergit.ast.token.LEFTSQUAREBRACKET;
import jp.kusumotolab.finergit.ast.token.LESS;
import jp.kusumotolab.finergit.ast.token.LINECOMMENT;
import jp.kusumotolab.finergit.ast.token.METHODREFERENCE;
import jp.kusumotolab.finergit.ast.token.ModifierFactory;
import jp.kusumotolab.finergit.ast.token.NEW;
import jp.kusumotolab.finergit.ast.token.NULL;
import jp.kusumotolab.finergit.ast.token.NUMBERLITERAL;
import jp.kusumotolab.finergit.ast.token.OR;
import jp.kusumotolab.finergit.ast.token.OperatorFactory;
import jp.kusumotolab.finergit.ast.token.PACKAGE;
import jp.kusumotolab.finergit.ast.token.PACKAGENAME;
import jp.kusumotolab.finergit.ast.token.PrimitiveTypeFactory;
import jp.kusumotolab.finergit.ast.token.QUESTION;
import jp.kusumotolab.finergit.ast.token.RETURN;
import jp.kusumotolab.finergit.ast.token.RIGHTARROW;
import jp.kusumotolab.finergit.ast.token.RIGHTBRACKET;
import jp.kusumotolab.finergit.ast.token.RIGHTPAREN;
import jp.kusumotolab.finergit.ast.token.RIGHTSQUAREBRACKET;
import jp.kusumotolab.finergit.ast.token.SEMICOLON;
import jp.kusumotolab.finergit.ast.token.STATIC;
import jp.kusumotolab.finergit.ast.token.STRINGLITERAL;
import jp.kusumotolab.finergit.ast.token.SUPER;
import jp.kusumotolab.finergit.ast.token.SWITCH;
import jp.kusumotolab.finergit.ast.token.SYNCHRONIZED;
import jp.kusumotolab.finergit.ast.token.THIS;
import jp.kusumotolab.finergit.ast.token.THROW;
import jp.kusumotolab.finergit.ast.token.THROWS;
import jp.kusumotolab.finergit.ast.token.TRY;
import jp.kusumotolab.finergit.ast.token.TYPENAME;
import jp.kusumotolab.finergit.ast.token.VARIABLENAME;
import jp.kusumotolab.finergit.ast.token.WHILE;

public class JavaFileVisitor extends ASTVisitor {

  public final Path path;
  private final Stack<FinerJavaModule> moduleStack;
  private final List<FinerJavaModule> moduleList;
  private final Stack<Class<?>> contexts;

  public JavaFileVisitor(final Path path) {

    this.path = path;
    this.moduleStack = new Stack<>();
    this.moduleList = new ArrayList<>();
    this.contexts = new Stack<>();

    final Path parent = path.getParent();
    final String fileName = FilenameUtils.getBaseName(path.toString());
    final FinerJavaFile finerJavaFile = new FinerJavaFile(parent, fileName);
    this.moduleStack.push(finerJavaFile);
    this.moduleList.add(finerJavaFile);
  }

  public List<FinerJavaModule> getFinerJavaModules() {
    return this.moduleList;
  }

  @Override
  public boolean visit(final AnnotationTypeDeclaration node) {

    final Javadoc javadoc = node.getJavadoc();
    if (null != javadoc) {
      this.moduleStack.peek()
          .addToken(new JAVADOCCOMMENT(this.removeTerminalLineCharacter(javadoc.toString())));
    }

    // 修飾子の処理
    for (final Object modifier : node.modifiers()) {
      final JavaToken modifierToken = ModifierFactory.create(modifier.toString());
      this.moduleStack.peek().addToken(modifierToken);
    }

    this.contexts.push(CLASSNAME.class);
    node.getName().accept(this);
    final Class<?> context = this.contexts.pop();
    assert CLASSNAME.class == context : "error happened at JavaFileVisitor#visit(AnnotationTypeDeclaration)";

    this.moduleStack.peek().addToken(new LEFTBRACKET());

    final List<?> bodies = node.bodyDeclarations();
    for (final Object body : bodies) {
      ((BodyDeclaration) body).accept(this);
    }

    this.moduleStack.peek().addToken(new RIGHTBRACKET());

    return false;
  }

  @Override
  public boolean visit(final AnnotationTypeMemberDeclaration node) {

    // Javadoc コメントの処理
    final Javadoc javadoc = node.getJavadoc();
    if (null != javadoc) {
      this.moduleStack.peek()
          .addToken(new JAVADOCCOMMENT(this.removeTerminalLineCharacter(javadoc.toString())));
    }

    // 修飾子の処理
    for (final Object modifier : node.modifiers()) {
      final JavaToken modifierToken = ModifierFactory.create(modifier.toString());
      this.moduleStack.peek().addToken(modifierToken);
    }

    node.getType().accept(this);

    this.contexts.push(VARIABLENAME.class);
    node.getName().accept(this);
    final Class<?> context = this.contexts.pop();
    assert VARIABLENAME.class == context : "error happened at JavaFileVisitor#visit(AnnotationTypeMemberDeclaration)";

    final Expression defaultValue = node.getDefault();
    if (null != defaultValue) {
      this.moduleStack.peek().addToken(new ASSIGN());
      defaultValue.accept(this);
    }

    this.moduleStack.peek().addToken(new SEMICOLON());

    return false;
  }


  @Override
  public boolean visit(final AnonymousClassDeclaration node) {

    this.moduleStack.peek().addToken(new LEFTBRACKET());

    final List<?> bodies = node.bodyDeclarations();
    for (final Object body : bodies) {
      ((BodyDeclaration) body).accept(this);
    }

    this.moduleStack.peek().addToken(new RIGHTBRACKET());

    return false;
  }

  @Override
  public boolean visit(final ArrayAccess node) {

    node.getArray().accept(this);

    this.moduleStack.peek().addToken(new LEFTSQUAREBRACKET());

    node.getIndex().accept(this);

    this.moduleStack.peek().addToken(new RIGHTSQUAREBRACKET());

    return false;
  }

  @Override
  public boolean visit(final ArrayCreation node) {

    this.moduleStack.peek().addToken(new NEW());

    node.getType().accept(this);

    final ArrayInitializer initializer = node.getInitializer();
    if (null != initializer) {
      initializer.accept(this);
    }

    return false;
  }

  @Override
  public boolean visit(final ArrayInitializer node) {

    this.moduleStack.peek().addToken(new LEFTBRACKET());

    final List<?> expressions = node.expressions();
    if (null != expressions && !expressions.isEmpty()) {
      ((Expression) expressions.get(0)).accept(this);
      for (int index = 1; index < expressions.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((Expression) expressions.get(index)).accept(this);
      }
    }

    this.moduleStack.peek().addToken(new RIGHTBRACKET());

    return false;
  }

  // 変更の必要なし
  @Override
  public boolean visit(final ArrayType node) {
    return super.visit(node);
  }

  @Override
  public boolean visit(final AssertStatement node) {

    this.moduleStack.peek().addToken(new ASSERT());

    node.getExpression().accept(this);

    this.moduleStack.peek().addToken(new COLON());

    final Expression message = node.getMessage();
    if (null != message) {
      message.accept(this);
    }

    this.moduleStack.peek().addToken(new SEMICOLON());

    return false;
  }

  @Override
  public boolean visit(final Assignment node) {

    node.getLeftHandSide().accept(this);

    this.moduleStack.peek().addToken(new ASSIGN());

    node.getRightHandSide().accept(this);

    return false;
  }

  @Override
  public boolean visit(final Block node) {

    this.moduleStack.peek().addToken(new LEFTBRACKET());

    final List<?> statements = node.statements();
    for (final Object statement : statements) {
      ((Statement) statement).accept(this);
    }

    this.moduleStack.peek().addToken(new RIGHTBRACKET());

    return false;
  }

  @Override
  public boolean visit(final BlockComment node) {
    this.moduleStack.peek().addToken(new BLOCKCOMMENT(node.toString()));
    return false;
  }

  @Override
  public boolean visit(final BooleanLiteral node) {
    this.moduleStack.peek().addToken(BooleanLiteralFactory.create(node.toString()));
    return false;
  }

  @Override
  public boolean visit(final BreakStatement node) {

    this.moduleStack.peek().addToken(new BREAK());

    final SimpleName label = node.getLabel();
    if (null != label) {
      this.contexts.push(STRINGLITERAL.class);
      label.accept(this);
      final Class<?> context = this.contexts.pop();
      assert STRINGLITERAL.class == context : "error happend at visit(BreakStatement)";
    }

    this.moduleStack.peek().addToken(new SEMICOLON());

    return false;
  }

  @Override
  public boolean visit(final CastExpression node) {

    this.moduleStack.peek().addToken(new LEFTPAREN());

    node.getType().accept(this);

    this.moduleStack.peek().addToken(new RIGHTPAREN());

    node.getExpression().accept(this);

    return false;
  }

  @Override
  public boolean visit(final CatchClause node) {

    this.moduleStack.peek().addToken(new CATCH());

    this.moduleStack.peek().addToken(new LEFTPAREN());

    node.getException().accept(this);

    this.moduleStack.peek().addToken(new RIGHTPAREN());

    node.getBody().accept(this);

    return false;
  }

  @Override
  public boolean visit(final CharacterLiteral node) {

    final String literal = node.getEscapedValue();
    this.moduleStack.peek().addToken(new CHARLITERAL(literal));

    return false;
  }

  @Override
  public boolean visit(final ClassInstanceCreation node) {

    final Expression expression = node.getExpression();
    if (null != expression) {
      expression.accept(this);
      this.moduleStack.peek().addToken(new DOT());
    }

    this.moduleStack.peek().addToken(new NEW());

    node.getType().accept(this);

    this.moduleStack.peek().addToken(new LEFTPAREN());

    final List<?> arguments = node.arguments();
    if (null != arguments && !arguments.isEmpty()) {
      ((Expression) arguments.get(0)).accept(this);
      for (int index = 1; index < arguments.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((Expression) arguments.get(index)).accept(this);
      }
    }

    this.moduleStack.peek().addToken(new RIGHTPAREN());

    final AnonymousClassDeclaration acd = node.getAnonymousClassDeclaration();
    if (null != acd) {
      acd.accept(this);
    }

    return false;
  }

  // 変更の必要なし
  @Override
  public boolean visit(final CompilationUnit node) {
    return super.visit(node);
  }

  @Override
  public boolean visit(final ConditionalExpression node) {

    node.getExpression().accept(this);

    this.moduleStack.peek().addToken(new QUESTION());

    node.getThenExpression().accept(this);

    this.moduleStack.peek().addToken(new COLON());

    node.getElseExpression().accept(this);

    return false;
  }

  @Override
  public boolean visit(final ConstructorInvocation node) {

    this.moduleStack.peek().addToken(new THIS());
    this.moduleStack.peek().addToken(new LEFTPAREN());

    final List<?> arguments = node.arguments();
    if (null != arguments && !arguments.isEmpty()) {
      ((Expression) arguments.get(0)).accept(this);
      for (int index = 1; index < arguments.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((Expression) arguments.get(index)).accept(this);
      }
    }

    this.moduleStack.peek().addToken(new RIGHTPAREN());
    this.moduleStack.peek().addToken(new SEMICOLON());

    return false;
  }

  @Override
  public boolean visit(final ContinueStatement node) {

    this.moduleStack.peek().addToken(new CONTINUE());

    final SimpleName label = node.getLabel();
    if (null != label) {
      this.contexts.push(STRINGLITERAL.class);
      label.accept(this);
      final Class<?> context = this.contexts.pop();
      assert STRINGLITERAL.class == context : "error happend at visit(ContinueStatement)";
    }

    this.moduleStack.peek().addToken(new SEMICOLON());

    return false;
  }

  @Override
  public boolean visit(final CreationReference node) {

    node.getType().accept(this);

    this.moduleStack.peek().addToken(new METHODREFERENCE());

    this.moduleStack.peek().addToken(new NEW());

    return false;
  }

  @Override
  public boolean visit(final Dimension node) {

    this.moduleStack.peek().addToken(new LEFTSQUAREBRACKET());

    final List<?> annotations = node.annotations();
    if (null != annotations && !annotations.isEmpty()) {
      ((Annotation) annotations.get(0)).accept(this);
      for (int index = 1; index < annotations.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((Annotation) annotations.get(index)).accept(this);
      }
    }

    this.moduleStack.peek().addToken(new RIGHTSQUAREBRACKET());

    return false;
  }

  @Override
  public boolean visit(final DoStatement node) {

    this.moduleStack.peek().addToken(new DO());

    node.getBody().accept(this);

    this.moduleStack.peek().addToken(new WHILE());
    this.moduleStack.peek().addToken(new LEFTPAREN());

    node.getExpression().accept(this);

    this.moduleStack.peek().addToken(new RIGHTPAREN());
    this.moduleStack.peek().addToken(new SEMICOLON());

    return false;
  }

  @Override
  public boolean visit(final EmptyStatement node) {
    this.moduleStack.peek().addToken(new SEMICOLON());
    return false;
  }

  @Override
  public boolean visit(final EnhancedForStatement node) {

    this.moduleStack.peek().addToken(new FOR());
    this.moduleStack.peek().addToken(new LEFTPAREN());

    node.getParameter().accept(this);

    this.moduleStack.peek().addToken(new COLON());

    node.getExpression().accept(this);

    this.moduleStack.peek().addToken(new RIGHTPAREN());

    node.getBody().accept(this);

    return false;
  }

  @Override
  public boolean visit(final EnumConstantDeclaration node) {

    // Javadoc コメントの処理
    final Javadoc javadoc = node.getJavadoc();
    if (null != javadoc) {
      this.moduleStack.peek()
          .addToken(new JAVADOCCOMMENT(this.removeTerminalLineCharacter(javadoc.toString())));
    }

    // 修飾子の処理
    for (final Object modifier : node.modifiers()) {
      final JavaToken modifierToken = ModifierFactory.create(modifier.toString());
      this.moduleStack.peek().addToken(modifierToken);
    }

    this.contexts.push(CLASSNAME.class);
    node.getName().accept(this);
    final Class<?> context = this.contexts.pop();
    assert CLASSNAME.class == context : "error happend at JavaFileVisitor#visit(EnumConstantDeclaration)";

    final List<?> arguments = node.arguments();
    if (null != arguments && !arguments.isEmpty()) {

      this.moduleStack.peek().addToken(new LEFTPAREN());

      ((Expression) arguments.get(0)).accept(this);
      for (int index = 1; index < arguments.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((Expression) arguments.get(index)).accept(this);
      }

      this.moduleStack.peek().addToken(new RIGHTPAREN());
    }

    final AnonymousClassDeclaration acd = node.getAnonymousClassDeclaration();
    if (acd != null) {
      acd.accept(this);
    }

    return false;
  }

  @Override
  public boolean visit(final EnumDeclaration node) {

    // Javadoc コメントの処理
    final Javadoc javadoc = node.getJavadoc();
    if (null != javadoc) {
      this.moduleStack.peek()
          .addToken(new JAVADOCCOMMENT(this.removeTerminalLineCharacter(javadoc.toString())));
    }

    // 修飾子の処理
    for (final Object modifier : node.modifiers()) {
      final JavaToken modifierToken = ModifierFactory.create(modifier.toString());
      this.moduleStack.peek().addToken(modifierToken);
    }

    this.contexts.push(CLASSNAME.class);
    node.getName().accept(this);
    final Class<?> context = this.contexts.pop();
    assert CLASSNAME.class == context : "error happend at JavaFileVisitor#visit(EnumDeclaration)";

    this.moduleStack.peek().addToken(new LEFTBRACKET());

    for (final Object enumConstant : node.enumConstants()) {
      ((EnumConstantDeclaration) enumConstant).accept(this);
    }

    this.moduleStack.peek().addToken(new RIGHTBRACKET());

    return false;
  }

  // TODO テストできていない
  @Override
  public boolean visit(ExportsDirective node) {
    System.err.println("JavaFileVisitor#visit(ExportsDirective) is not implemented yet.");
    return super.visit(node);
  }

  @Override
  public boolean visit(final ExpressionMethodReference node) {

    node.getExpression().accept(this);

    this.moduleStack.peek().addToken(new METHODREFERENCE());

    this.contexts.push(INVOKEDMETHODNAME.class);
    node.getName().accept(this);
    final Class<?> context = this.contexts.pop();
    assert INVOKEDMETHODNAME.class == context : "error happened at visit(ExpressionMethodReference)";

    return false;
  }

  @Override
  public boolean visit(final ExpressionStatement node) {

    node.getExpression().accept(this);

    this.moduleStack.peek().addToken(new SEMICOLON());

    return false;
  }

  @Override
  public boolean visit(final FieldAccess node) {

    node.getExpression().accept(this);

    this.moduleStack.peek().addToken(new DOT());

    this.contexts.push(VARIABLENAME.class);
    node.getName().accept(this);
    final Class<?> context = this.contexts.pop();
    assert VARIABLENAME.class == context : "error happened at visit(FieldAccess)";

    return false;
  }

  @Override
  public boolean visit(final FieldDeclaration node) {

    // Javadoc コメントの処理
    final Javadoc javadoc = node.getJavadoc();
    if (null != javadoc) {
      this.moduleStack.peek()
          .addToken(new JAVADOCCOMMENT(this.removeTerminalLineCharacter(javadoc.toString())));
    }

    // 修飾子の処理
    for (final Object modifier : node.modifiers()) {
      final JavaToken modifierToken = ModifierFactory.create(modifier.toString());
      this.moduleStack.peek().addToken(modifierToken);
    }

    node.getType().accept(this);

    for (final Object fragment : node.fragments()) {
      ((VariableDeclarationFragment) fragment).accept(this);
    }

    this.moduleStack.peek().addToken(new SEMICOLON());

    return false;
  }

  @Override
  public boolean visit(final ForStatement node) {

    this.moduleStack.peek().addToken(new FOR());
    this.moduleStack.peek().addToken(new LEFTPAREN());

    // 初期化子の処理
    final List<?> initializers = node.initializers();
    if (null != initializers && !initializers.isEmpty()) {
      ((Expression) initializers.get(0)).accept(this);
      for (int index = 1; index < initializers.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((Expression) initializers.get(index)).accept(this);
      }
    }

    this.moduleStack.peek().addToken(new SEMICOLON());

    // 条件節の処理
    final Expression condition = node.getExpression();
    if (null != condition) {
      condition.accept(this);
    }

    this.moduleStack.peek().addToken(new SEMICOLON());

    // 更新子の処理
    final List<?> updaters = node.updaters();
    if (null != updaters && !updaters.isEmpty()) {
      ((Expression) updaters.get(0)).accept(this);
      for (int index = 1; index < updaters.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((Expression) updaters.get(index)).accept(this);
      }
    }

    this.moduleStack.peek().addToken(new LEFTPAREN());

    final Statement body = node.getBody();
    if (null != body) {
      body.accept(this);
    }

    return false;

  }

  @Override
  public boolean visit(final IfStatement node) {

    this.moduleStack.peek().addToken(new IF());
    this.moduleStack.peek().addToken(new LEFTPAREN());

    node.getExpression().accept(this);

    this.moduleStack.peek().addToken(new RIGHTPAREN());

    final Statement thenStatement = node.getThenStatement();
    if (null != thenStatement) {
      thenStatement.accept(this);
    }

    final Statement elseStatement = node.getElseStatement();
    if (null != elseStatement) {
      this.moduleStack.peek().addToken(new ELSE());
      elseStatement.accept(this);
    }

    return false;
  }

  @Override
  public boolean visit(final ImportDeclaration node) {

    if (node.isStatic()) {
      this.moduleStack.peek().addToken(new STATIC());
    }

    this.moduleStack.peek().addToken(new IMPORT());

    this.contexts.push(IMPORTNAME.class);
    node.getName().accept(this);
    final Class<?> c = this.contexts.pop();
    assert c == IMPORTNAME.class : "context error.";

    return false;
  }

  @Override
  public boolean visit(final InfixExpression node) {

    node.getLeftOperand().accept(this);

    final Operator operator = node.getOperator();
    final JavaToken operatorToken = OperatorFactory.create(operator.toString());
    this.moduleStack.peek().addToken(operatorToken);

    node.getRightOperand().accept(this);

    final List<?> extendedOperands = node.extendedOperands();
    for (int index = 0; index < extendedOperands.size(); index++) {
      this.moduleStack.peek().addToken(operatorToken);
      ((Expression) extendedOperands.get(index)).accept(this);
    }

    return false;
  }

  @Override
  public boolean visit(final Initializer node) {

    // Javadoc コメントの処理
    final Javadoc javadoc = node.getJavadoc();
    if (null != javadoc) {
      this.moduleStack.peek()
          .addToken(new JAVADOCCOMMENT(this.removeTerminalLineCharacter(javadoc.toString())));
    }

    for (final Object modifier : node.modifiers()) {
      final JavaToken modifierToken = ModifierFactory.create(modifier.toString());
      this.moduleStack.peek().addToken(modifierToken);
    }

    node.getBody().accept(this);

    return false;
  }

  @Override
  public boolean visit(final InstanceofExpression node) {

    node.getLeftOperand().accept(this);

    this.moduleStack.peek().addToken(new INSTANCEOF());

    node.getRightOperand().accept(this);

    return false;
  }

  @Override
  public boolean visit(final IntersectionType node) {

    final List<?> types = node.types();
    ((Type) types.get(0)).accept(this);

    for (int index = 1; index < types.size(); index++) {
      this.moduleStack.peek().addToken(new AND());
      ((Type) types.get(index)).accept(this);
    }

    return false;
  }

  @Override
  public boolean visit(final Javadoc node) {
    this.moduleStack.peek()
        .addToken(new JAVADOCCOMMENT(this.removeTerminalLineCharacter(node.toString())));
    return false;
  }

  @Override
  public boolean visit(final LabeledStatement node) {

    this.contexts.push(LABELNAME.class);
    node.getLabel().accept(this);
    final Class<?> context = this.contexts.pop();
    assert LABELNAME.class == context : "error happened at JavaFileVisitor#visit(LabeledStatement)";

    this.moduleStack.peek().addToken(new COLON());

    node.getBody().accept(this);

    return false;
  }

  @Override
  public boolean visit(final LambdaExpression node) {

    if (node.hasParentheses()) {
      this.moduleStack.peek().addToken(new LEFTPAREN());
    }

    final List<?> parameters = node.parameters();
    if (null != parameters && !parameters.isEmpty()) {
      ((VariableDeclaration) parameters.get(0)).accept(this);
      for (int index = 1; index < parameters.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((VariableDeclaration) parameters.get(index)).accept(this);
      }
    }

    if (node.hasParentheses()) {
      this.moduleStack.peek().addToken(new RIGHTPAREN());
    }

    this.moduleStack.peek().addToken(new RIGHTARROW());

    node.getBody().accept(this);

    return false;
  }

  @Override
  public boolean visit(final LineComment node) {
    this.moduleStack.peek().addToken(new LINECOMMENT(node.toString()));
    return false;
  }

  @Override
  public boolean visit(final MarkerAnnotation node) {

    this.moduleStack.peek().addToken(new ANNOTATION(node.toString()));
    return false;
  }

  // TODO テストできていない
  @Override
  public boolean visit(MemberRef node) {
    System.err.println("JavaFileVisitor#visit(MemberRef) is not implemented yet.");
    return super.visit(node);
  }

  // TODO テストできていない
  @Override
  public boolean visit(MemberValuePair node) {
    System.err.println("JavaFileVisitor#visit(MemberValuePair) is not implemented yet.");
    return super.visit(node);
  }

  // TODO テストできていない
  @Override
  public boolean visit(MethodRef node) {
    System.err.println("JavaFileVisitor#visit(MemberRef) is not implemented yet.");
    return super.visit(node);
  }

  // TODO テストできていない
  @Override
  public boolean visit(MethodRefParameter node) {
    System.err.println("JavaFileVisitor#visit(MethodRefParameter) is not implemented yet.");
    return super.visit(node);
  }

  @Override
  public boolean visit(final MethodDeclaration node) {

    { // ダミーメソッドを生成し，モジュールスタックに追加
      final FinerJavaModule outerModule = this.moduleStack.peek();
      final FinerJavaMethod dummyMethod = new FinerJavaMethod("DummyMethod", outerModule);
      this.moduleStack.push(dummyMethod);
    }

    // Javadoc コメントの処理
    final Javadoc javadoc = node.getJavadoc();
    if (null != javadoc) {
      this.moduleStack.peek()
          .addToken(new JAVADOCCOMMENT(this.removeTerminalLineCharacter(javadoc.toString())));
    }

    // 修飾子の処理（ダミーメソッドに追加）
    for (final Object modifier : node.modifiers()) {
      final JavaToken modifierToken = ModifierFactory.create(modifier.toString());
      this.moduleStack.peek().addToken(modifierToken);
    }

    // 返り値の処理（ダミーメソッドに追加）
    final Type returnType = node.getReturnType2();
    if (null != returnType) { // コンストラクタのときは returnType が null
      this.contexts.push(TYPENAME.class);
      node.getReturnType2().accept(this);
      final Class<?> context = this.contexts.pop();
      assert TYPENAME.class == context : "error happend at visit(MethodDeclaration)";
    }

    {// メソッド名の処理（ダミーメソッドに追加）
      this.contexts.push(DECLAREDMETHODNAME.class);
      node.getName().accept(this);
      final Class<?> context = this.contexts.pop();
      assert DECLAREDMETHODNAME.class == context : "error happend at visit(MethodDeclaration)";
    }

    // "(" の処理（ダミーメソッドに追加）
    this.moduleStack.peek().addToken(new LEFTPAREN());

    // 引数の処理（ダミーメソッドに追加）
    final List<?> parameters = node.parameters();
    if (null != parameters && !parameters.isEmpty()) {
      ((SingleVariableDeclaration) parameters.get(0)).accept(this);
      for (int index = 1; index < parameters.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((SingleVariableDeclaration) parameters.get(index)).accept(this);
      }
    }

    // ")" の処理（ダミーメソッドに追加）
    this.moduleStack.peek().addToken(new RIGHTPAREN());

    // throws 節の処理
    final List<?> exceptions = node.thrownExceptionTypes();
    if (null != exceptions && !exceptions.isEmpty()) {
      this.moduleStack.peek().addToken(new THROWS());
      this.contexts.push(TYPENAME.class);
      ((Type) exceptions.get(0)).accept(this);
      for (int index = 1; index < exceptions.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((Type) exceptions.get(index)).accept(this);
      }
      final Class<?> context = this.contexts.pop();
      assert TYPENAME.class == context : "error happened at visit(MethodDeclaration)";
    }

    // メソッドモジュールの名前を生成
    final StringBuilder text = new StringBuilder();
    final String methodName = node.getName().getIdentifier();
    text.append(methodName);
    text.append("(");
    final List<String> types = new ArrayList<>();
    for (final Object parameter : node.parameters()) {
      final SingleVariableDeclaration svd = (SingleVariableDeclaration) parameter;
      final String type = svd.getType().toString();
      types.add(type);
    }
    text.append(String.join(",", types));
    text.append(")");

    // ダミーメソッドをスタックから取り除く
    final FinerJavaModule dummyMethod = this.moduleStack.pop();

    // 内部クラス内のメソッドかどうかの判定
    final FinerJavaModule peekModule = this.moduleStack.peek();
    final boolean isInnerMethod =
        1 < this.moduleStack.size() && peekModule instanceof FinerJavaMethod;

    final FinerJavaMethod methodModule;
    if (!isInnerMethod) { // 内部クラス内のメソッドではないとき
      final FinerJavaModule outerModule = this.moduleStack.peek();
      methodModule = new FinerJavaMethod(text.toString(), outerModule);
      this.moduleStack.push(methodModule);
      this.moduleList.add(methodModule);
    }

    else { // 内部クラスのメソッドのとき
      methodModule = (FinerJavaMethod) peekModule;
    }

    // ダミーメソッド内のトークンを新しいメソッドモジュールに移行
    dummyMethod.getTokens().stream().forEach(methodModule::addToken);

    // メソッドの中身の処理
    final Block body = node.getBody();
    if (null != body) {
      body.accept(this);
    } else {
      this.moduleStack.peek().addToken(new SEMICOLON());
    }

    if (!isInnerMethod) { // 内部クラス内のメソッドではないとき
      final FinerJavaMethod finerJavaMethod = (FinerJavaMethod) this.moduleStack.pop();
      this.moduleStack.peek().addToken(
          new FinerJavaMethodToken("MetodToken[" + finerJavaMethod.name + "]", finerJavaMethod));
    }

    return false;
  }

  @Override
  public boolean visit(final MethodInvocation node) {

    final Expression qualifier = node.getExpression();
    if (null != qualifier) {
      qualifier.accept(this);
      this.moduleStack.peek().addToken(new DOT());
    }

    this.contexts.push(INVOKEDMETHODNAME.class);
    node.getName().accept(this);
    final Class<?> context = this.contexts.pop();
    assert INVOKEDMETHODNAME.class == context : "error happened at visit(MethodInvocation)";

    this.moduleStack.peek().addToken(new LEFTPAREN());

    final List<?> arguments = node.arguments();
    if (null != arguments && !arguments.isEmpty()) {
      ((Expression) arguments.get(0)).accept(this);
      for (int index = 1; index < arguments.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((Expression) arguments.get(index)).accept(this);
      }
    }

    this.moduleStack.peek().addToken(new RIGHTPAREN());

    return false;
  }

  // 変更の必要なし
  @Override
  public boolean visit(final Modifier node) {
    return super.visit(node);
  }

  // TODO テストできていない
  @Override
  public boolean visit(ModuleDeclaration node) {
    System.err.println("JavaFileVisitor#visit(ModuleDeclaration) is not implemented yet.");
    return super.visit(node);
  }

  // TODO テストできていない
  @Override
  public boolean visit(ModuleModifier node) {
    System.err.println("JavaFileVisitor#visit(ModuleModifier) is not implemented yet.");
    return super.visit(node);
  }

  @Override
  public boolean visit(final NameQualifiedType node) {

    this.contexts.push(INVOKEDMETHODNAME.class);
    node.getQualifier().accept(this);
    final Class<?> qualifierText = this.contexts.pop();
    assert INVOKEDMETHODNAME.class == qualifierText : "error happened at visit(NameQualifiedType)";

    this.moduleStack.peek().addToken(new DOT());

    for (final Object annotation : node.annotations()) {
      ((Annotation) annotation).accept(this);
    }

    this.contexts.push(INVOKEDMETHODNAME.class);
    node.getName().accept(this);
    final Class<?> nameContext = this.contexts.pop();
    assert INVOKEDMETHODNAME.class == nameContext : "error happened at visit(NameQualifiedType)";

    return false;
  }

  // TODO テストできていない
  @Override
  public boolean visit(NormalAnnotation node) {

    System.err.println("JavaFileVisitor#visit(NormalAnnotation) is not implemented yet.");
    return super.visit(node);
  }

  @Override
  public boolean visit(final NullLiteral node) {
    this.moduleStack.peek().addToken(new NULL());
    return false;
  }

  @Override
  public boolean visit(final NumberLiteral node) {
    this.moduleStack.peek().addToken(new NUMBERLITERAL(node.getToken()));
    return false;
  }

  // TODO テストできていない
  @Override
  public boolean visit(OpensDirective node) {
    System.err.println("JavaFileVisitor#visit(OpensDirective) is not implemented yet.");
    return super.visit(node);
  }

  @Override
  public boolean visit(final PackageDeclaration node) {

    this.moduleStack.peek().addToken(new PACKAGE());

    this.contexts.push(PACKAGENAME.class);
    node.getName().accept(this);
    final Class<?> context = this.contexts.pop();
    assert PACKAGENAME.class == context : "context error at JavaFileVisitor#visit(PackageDeclaration)";

    return false;
  }

  @Override
  public boolean visit(final ParameterizedType node) {

    node.getType().accept(this);

    this.moduleStack.peek().addToken(new LESS());

    final List<?> typeArguments = node.typeArguments();
    if (null != typeArguments && !typeArguments.isEmpty()) {
      ((Type) typeArguments.get(0)).accept(this);
      for (int index = 1; index < typeArguments.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((Type) typeArguments.get(index)).accept(this);
      }
    }

    this.moduleStack.peek().addToken(new GREAT());

    return false;
  }

  @Override
  public boolean visit(final ParenthesizedExpression node) {

    this.moduleStack.peek().addToken(new LEFTPAREN());

    node.getExpression().accept(this);

    this.moduleStack.peek().addToken(new RIGHTPAREN());

    return false;
  }

  @Override
  public boolean visit(final PostfixExpression node) {

    node.getOperand().accept(this);

    final PostfixExpression.Operator operator = node.getOperator();
    OperatorFactory.create(operator.toString());

    return false;
  }

  @Override
  public boolean visit(final PrefixExpression node) {

    final PrefixExpression.Operator operator = node.getOperator();
    OperatorFactory.create(operator.toString());

    node.getOperand().accept(this);

    return false;
  }

  // TODO テストできていない
  @Override
  public boolean visit(ProvidesDirective node) {
    System.err.println("JavaFileVisitor#visit(ProvidesDirective) is not implemented yet.");
    return super.visit(node);
  }

  @Override
  public boolean visit(final PrimitiveType node) {

    final JavaToken primitiveTypeToken =
        PrimitiveTypeFactory.create(node.getPrimitiveTypeCode().toString());
    this.moduleStack.peek().addToken(primitiveTypeToken);

    return super.visit(node);
  }

  @Override
  public boolean visit(final QualifiedName node) {

    final Name qualifier = node.getQualifier();
    qualifier.accept(this);

    this.moduleStack.peek().addToken(new DOT());

    final SimpleName name = node.getName();
    name.accept(this);

    return false;
  }

  @Override
  public boolean visit(final QualifiedType node) {

    node.getQualifier().accept(this);

    this.moduleStack.peek().addToken(new DOT());

    for (final Object annotation : node.annotations()) {
      ((Annotation) annotation).accept(this);
    }

    this.contexts.push(TYPENAME.class);
    node.getName().accept(this);
    final Class<?> context = this.contexts.pop();
    assert TYPENAME.class == context : "error happened at JavaFileVisitor#visit(QualifiedType)";

    return false;
  }

  // TODO テストできていない
  @Override
  public boolean visit(RequiresDirective node) {
    System.err.println("JavaFileVisitor#visit(RequiresDirective) is not implemented yet.");
    return super.visit(node);
  }

  @Override
  public boolean visit(final ReturnStatement node) {

    this.moduleStack.peek().addToken(new RETURN());

    final Expression expression = node.getExpression();
    if (null != expression) {
      expression.accept(this);
    }

    this.moduleStack.peek().addToken(new SEMICOLON());

    return false;
  }

  @Override
  public boolean visit(final SimpleName node) {

    final String identifier = node.getIdentifier();

    if (this.contexts.isEmpty()) {
      this.moduleStack.peek().addToken(new VARIABLENAME(identifier));
      return false;
    }

    final Class<?> context = this.contexts.peek();
    if (VARIABLENAME.class == context) {
      this.moduleStack.peek().addToken(new VARIABLENAME(identifier));
    }

    else if (TYPENAME.class == context) {
      this.moduleStack.peek().addToken(new TYPENAME(identifier));
    }

    else if (DECLAREDMETHODNAME.class == context) {
      this.moduleStack.peek().addToken(new DECLAREDMETHODNAME(identifier));
    }

    else if (INVOKEDMETHODNAME.class == context) {
      this.moduleStack.peek().addToken(new INVOKEDMETHODNAME(identifier));
    }

    else if (PACKAGENAME.class == context) {
      this.moduleStack.peek().addToken(new PACKAGENAME(identifier));
    }

    else if (IMPORTNAME.class == context) {
      this.moduleStack.peek().addToken(new IMPORTNAME(identifier));
    }

    else if (LABELNAME.class == context) {
      this.moduleStack.peek().addToken(new LABELNAME(identifier));
    }

    return false;
  }

  @Override
  public boolean visit(final SimpleType node) {
    this.contexts.push(TYPENAME.class);
    node.getName().accept(this);
    final Class<?> context = this.contexts.pop();
    assert TYPENAME.class == context : "error happend at visit(SimpleType)";

    return false;
  }

  @Override
  public boolean visit(final SingleMemberAnnotation node) {

    this.moduleStack.peek().addToken(new ANNOTATION(node.toString()));
    return false;
  }

  @Override
  public boolean visit(final SingleVariableDeclaration node) {

    // 修飾子の処理
    for (final Object modifier : node.modifiers()) {
      final JavaToken modifierToken = ModifierFactory.create(modifier.toString());
      this.moduleStack.peek().addToken(modifierToken);
    }

    // 型の処理
    node.getType().accept(this);

    {// 変数名の処理
      this.contexts.push(VARIABLENAME.class);
      node.getName().accept(this);
      final Class<?> context = this.contexts.pop();
      assert VARIABLENAME.class == context : "error happend at visit(SingleVariableDeclaration";
    }

    return false;
  }

  @Override
  public boolean visit(final StringLiteral node) {
    this.moduleStack.peek().addToken(new STRINGLITERAL(node.getLiteralValue()));
    return false;
  }

  @Override
  public boolean visit(final SuperConstructorInvocation node) {

    final Expression qualifier = node.getExpression();
    if (null != qualifier) {
      qualifier.accept(this);
      this.moduleStack.peek().addToken(new DOT());
    }

    this.moduleStack.peek().addToken(new SUPER());
    this.moduleStack.peek().addToken(new LEFTPAREN());

    final List<?> arguments = node.arguments();
    if (null != arguments && !arguments.isEmpty()) {
      ((Expression) arguments.get(0)).accept(this);
      for (int index = 1; index < arguments.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((Expression) arguments.get(index)).accept(this);
      }
    }

    this.moduleStack.peek().addToken(new RIGHTPAREN());
    this.moduleStack.peek().addToken(new SEMICOLON());

    return false;
  }

  @Override
  public boolean visit(final SuperFieldAccess node) {

    this.moduleStack.peek().addToken(new SUPER());
    this.moduleStack.peek().addToken(new DOT());

    this.contexts.push(VARIABLENAME.class);
    node.getName().accept(this);
    final Class<?> context = this.contexts.pop();
    assert VARIABLENAME.class == context : "error happend at visit(SuperFieldAccess";

    return false;
  }

  // TODO node.getQualifier が null でない場合はテストできていない
  @Override
  public boolean visit(final SuperMethodInvocation node) {

    final Name qualifier = node.getQualifier();
    if (null != qualifier) {
      qualifier.accept(this);
      this.moduleStack.peek()
          .addToken(new DOT());
    }

    this.moduleStack.peek().addToken(new SUPER());
    this.moduleStack.peek().addToken(new DOT());

    this.contexts.push(INVOKEDMETHODNAME.class);
    node.getName().accept(this);

    final Class<?> context = this.contexts.pop();
    assert INVOKEDMETHODNAME.class == context : "error happend at JavaFileVisitor#visit(SuperMethodInvocation)";

    this.moduleStack.peek().addToken(new LEFTPAREN());

    final List<?> arguments = node.arguments();
    if (null != arguments && !arguments.isEmpty()) {
      ((Expression) arguments.get(0)).accept(this);
      for (int index = 1; index < arguments.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((Expression) arguments.get(index)).accept(this);
      }
    }

    this.moduleStack.peek().addToken(new RIGHTPAREN());

    return false;
  }

  // TODO テストできていない
  @Override
  public boolean visit(final SuperMethodReference node) {

    this.moduleStack.peek()
        .addToken(new SUPER());
    this.moduleStack.peek()
        .addToken(new METHODREFERENCE());

    this.contexts.push(INVOKEDMETHODNAME.class);
    node.getName()
        .accept(this);
    final Class<?> context = this.contexts.pop();
    assert INVOKEDMETHODNAME.class == context : "error happened at visit(SuperMethodReference)";

    return false;
  }

  @Override
  public boolean visit(final SwitchCase node) {

    final Expression expression = node.getExpression();

    // case のとき
    if (null != expression) {
      this.moduleStack.peek().addToken(new CASE());
      expression.accept(this);
    }

    // default のとき
    else {
      this.moduleStack.peek().addToken(new DEFAULT());
    }

    this.moduleStack.peek().addToken(new COLON());

    return false;
  }

  @Override
  public boolean visit(final SwitchStatement node) {

    this.moduleStack.peek().addToken(new SWITCH());
    this.moduleStack.peek().addToken(new LEFTPAREN());

    node.getExpression().accept(this);

    this.moduleStack.peek().addToken(new RIGHTPAREN());
    this.moduleStack.peek().addToken(new LEFTBRACKET());

    final List<?> statements = node.statements();
    for (final Object statement : statements) {
      ((Statement) statement).accept(this);
    }

    this.moduleStack.peek().addToken(new RIGHTBRACKET());

    return false;
  }

  @Override
  public boolean visit(final SynchronizedStatement node) {

    this.moduleStack.peek().addToken(new SYNCHRONIZED());
    this.moduleStack.peek().addToken(new LEFTPAREN());

    node.getExpression().accept(this);

    this.moduleStack.peek().addToken(new RIGHTPAREN());

    node.getBody().accept(this);

    return false;
  }

  // TODO テストできていない
  @Override
  public boolean visit(TagElement node) {
    System.err.println("JavaFileVisitor#visit(TagElement) is not implemented yet.");
    return super.visit(node);
  }

  // TODO テストできていない
  @Override
  public boolean visit(TextElement node) {
    System.err.println("JavaFileVisitor#visit(TextElement) is not implemented yet.");
    return super.visit(node);
  }

  @Override
  public boolean visit(final ThisExpression node) {
    this.moduleStack.peek().addToken(new THIS());
    return false;
  }

  @Override
  public boolean visit(final ThrowStatement node) {

    this.moduleStack.peek().addToken(new THROW());

    node.getExpression().accept(this);

    this.moduleStack.peek().addToken(new SEMICOLON());

    return false;
  }

  @Override
  public boolean visit(final TryStatement node) {

    this.moduleStack.peek().addToken(new TRY());

    final List<?> resources = node.resources();
    if (null != resources && !resources.isEmpty()) {
      this.moduleStack.peek().addToken(new LEFTPAREN());

      ((Expression) resources.get(0)).accept(this);
      this.moduleStack.peek().addToken(new SEMICOLON());

      for (int index = 1; index < resources.size(); index++) {
        this.moduleStack.peek().addToken(new SEMICOLON());
        ((Expression) resources.get(index)).accept(this);
      }

      this.moduleStack.peek().addToken(new RIGHTPAREN());
    }

    node.getBody().accept(this);

    final List<?> catchClauses = node.catchClauses();
    for (final Object catchClause : catchClauses) {
      ((CatchClause) catchClause).accept(this);
    }

    final Block finallyBlock = node.getFinally();
    if (null != finallyBlock) {
      this.moduleStack.peek().addToken(new FINALLY());
      finallyBlock.accept(this);
    }

    return false;
  }

  @Override
  public boolean visit(final TypeDeclaration node) {

    final Javadoc javadoc = node.getJavadoc();
    if (null != javadoc) {
      this.moduleStack.peek()
          .addToken(new JAVADOCCOMMENT(this.removeTerminalLineCharacter(javadoc.toString())));
    }

    // 修飾子の処理
    for (final Object modifier : node.modifiers()) {
      final JavaToken modifierToken = ModifierFactory.create(modifier.toString());
      this.moduleStack.peek().addToken(modifierToken);
    }

    // "class"の処理
    this.moduleStack.peek().addToken(new CLASS());

    // クラス名の処理
    this.contexts.push(CLASSNAME.class);
    node.getName().accept(this);
    final Class<?> nameContext = this.contexts.pop();
    assert CLASSNAME.class == nameContext : "error happened at visit(TypeDeclaration)";

    // extends 節の処理
    final Type superType = node.getSuperclassType();
    if (null != superType) {
      this.moduleStack.peek().addToken(new EXTENDS());
      this.contexts.push(TYPENAME.class);
      superType.accept(this);
      final Class<?> extendsContext = this.contexts.pop();
      assert TYPENAME.class == extendsContext : "error happened at visit(TypeDeclaration)";
    }

    // implements 節の処理
    @SuppressWarnings("rawtypes")
    final List interfaces = node.superInterfaceTypes();
    if (null != interfaces && 0 < interfaces.size()) {

      this.contexts.push(TYPENAME.class);

      this.moduleStack.peek().addToken(new IMPLEMENTS());
      ((Type) interfaces.get(0)).accept(this);

      for (int index = 1; index < interfaces.size(); index++) {
        this.moduleStack.peek().addToken(new COMMA());
        ((Type) interfaces.get(index)).accept(this);
      }

      final Class<?> implementsContext = this.contexts.pop();
      assert TYPENAME.class == implementsContext : "error happened at visit(TypeDeclaration)";
    }

    // "{"の処理
    this.moduleStack.peek().addToken(new LEFTBRACKET());

    // 中身の処理
    for (final Object o : node.bodyDeclarations()) {
      final BodyDeclaration bodyDeclaration = (BodyDeclaration) o;
      bodyDeclaration.accept(this);
    }

    // "}"の処理
    this.moduleStack.peek().addToken(new RIGHTBRACKET());

    return false;
  }

  // 変更の必要なし
  @Override
  public boolean visit(final TypeDeclarationStatement node) {
    return super.visit(node);
  }

  @Override
  public boolean visit(final TypeLiteral node) {

    node.getType().accept(this);

    this.moduleStack.peek().addToken(new DOT());
    this.moduleStack.peek().addToken(new CLASS());

    return false;
  }

  @Override
  public boolean visit(final TypeMethodReference node) {

    node.getType().accept(this);

    this.moduleStack.peek().addToken(new METHODREFERENCE());

    this.contexts.push(INVOKEDMETHODNAME.class);
    node.getName().accept(this);
    final Class<?> context = this.contexts.pop();
    assert INVOKEDMETHODNAME.class == context : "error happened at visit(TypeMethodReference)";

    return super.visit(node);
  }

  // TODO テストできていない
  @Override
  public boolean visit(TypeParameter node) {
    System.err.println("JavaFileVisitor#visit(TypeParameter) is not implemented yet.");
    return super.visit(node);
  }

  @Override
  public boolean visit(final UnionType node) {

    final List<?> types = node.types();
    ((Type) types.get(0)).accept(this);

    for (int index = 1; index < types.size(); index++) {
      this.moduleStack.peek().addToken(new OR());
      ((Type) types.get(index)).accept(this);
    }

    return false;
  }

  // TODO テストできていない
  @Override
  public boolean visit(UsesDirective node) {
    System.err.println("JavaFileVisitor#visit(UsesDirective) is not implemented yet.");
    return super.visit(node);
  }

  @Override
  public boolean visit(final VariableDeclarationExpression node) {

    // 修飾子の処理
    for (final Object modifier : node.modifiers()) {
      final JavaToken modifierToken = ModifierFactory.create(modifier.toString());
      this.moduleStack.peek().addToken(modifierToken);
    }

    node.getType().accept(this);

    final List<?> fragments = node.fragments();
    ((VariableDeclarationFragment) fragments.get(0)).accept(this);
    for (int index = 1; index < fragments.size(); index++) {
      this.moduleStack.peek().addToken(new COMMA());
      ((VariableDeclarationFragment) fragments.get(index)).accept(this);
    }

    return false;
  }

  @Override
  public boolean visit(final VariableDeclarationStatement node) {

    // 修飾子の処理
    for (final Object modifier : node.modifiers()) {
      final JavaToken modifierToken = ModifierFactory.create(modifier.toString());
      this.moduleStack.peek().addToken(modifierToken);
    }

    node.getType().accept(this);

    final List<?> fragments = node.fragments();
    ((VariableDeclarationFragment) fragments.get(0)).accept(this);
    for (int index = 1; index < fragments.size(); index++) {
      this.moduleStack.peek().addToken(new COMMA());
      ((VariableDeclarationFragment) fragments.get(index)).accept(this);
    }

    this.moduleStack.peek().addToken(new SEMICOLON());

    return false;
  }

  @Override
  public boolean visit(final VariableDeclarationFragment node) {

    this.contexts.push(VARIABLENAME.class);
    node.getName().accept(this);
    final Class<?> context = this.contexts.pop();
    assert VARIABLENAME.class == context : "error happened at JavaFileVisitor#visit(VariableDeclarationFragment)";

    final Expression initializer = node.getInitializer();
    if (null != initializer) {
      this.moduleStack.peek().addToken(new ASSIGN());
      initializer.accept(this);
    }

    return false;
  }

  @Override
  public boolean visit(final WhileStatement node) {

    this.moduleStack.peek().addToken(new WHILE());
    this.moduleStack.peek().addToken(new LEFTPAREN());

    node.getExpression().accept(this);

    this.moduleStack.peek().addToken(new RIGHTPAREN());

    return false;
  }

  @Override
  public boolean visit(final WildcardType node) {
    this.moduleStack.peek().addToken(new QUESTION());
    return super.visit(node);
  }

  private String removeTerminalLineCharacter(final String text) {
    if (text.endsWith("\r\n")) {
      return this.removeTerminalLineCharacter(text.substring(0, text.length() - 2));
    } else if (text.endsWith("\r") || text.endsWith("\n")) {
      return this.removeTerminalLineCharacter(text.substring(0, text.length() - 1));
    } else
      return text;
  }
}