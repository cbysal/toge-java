// Generated from SysY.g4 by ANTLR 4.13.1
package compile.sysy;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SysYParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SysYVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SysYParser#root}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoot(SysYParser.RootContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#compUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompUnit(SysYParser.CompUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(SysYParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#dimensions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDimensions(SysYParser.DimensionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#varDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarDecl(SysYParser.VarDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#varDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarDef(SysYParser.VarDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#scalarVarDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScalarVarDef(SysYParser.ScalarVarDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#arrayVarDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayVarDef(SysYParser.ArrayVarDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#initVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitVal(SysYParser.InitValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#funcDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncDef(SysYParser.FuncDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#funcArg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncArg(SysYParser.FuncArgContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#blockStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockStmt(SysYParser.BlockStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt(SysYParser.StmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#assignStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignStmt(SysYParser.AssignStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#blankStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlankStmt(SysYParser.BlankStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#expStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpStmt(SysYParser.ExpStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#ifElseStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfElseStmt(SysYParser.IfElseStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#ifStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStmt(SysYParser.IfStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#whileStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStmt(SysYParser.WhileStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#breakStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreakStmt(SysYParser.BreakStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#continueStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinueStmt(SysYParser.ContinueStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#retStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRetStmt(SysYParser.RetStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#lVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLVal(SysYParser.LValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#unaryExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExp(SysYParser.UnaryExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#varExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarExp(SysYParser.VarExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#funcCallExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncCallExp(SysYParser.FuncCallExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#lorExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLorExp(SysYParser.LorExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#landExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLandExp(SysYParser.LandExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#equalityExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityExp(SysYParser.EqualityExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#relationalExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExp(SysYParser.RelationalExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#additiveExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExp(SysYParser.AdditiveExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#multiplicativeExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExp(SysYParser.MultiplicativeExpContext ctx);
}