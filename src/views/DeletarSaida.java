package views;

import java.util.Scanner;

import controllers.SaidaController;
import models.Saida;

public class DeletarSaida {

	private static Scanner print = new Scanner(System.in);
	private static Saida saida;

	public static void renderizar(){
		
		saida = new Saida();

		System.out.println("\nDigite o ID da saída que deseja deletar");
		int id = print.nextInt();
		saida = SaidaController.buscarPorId(id);
		if (saida != null){
			if(SaidaController.deletar(saida)){
				System.out.println("\nRegistro de saída excluído com sucesso");
			}
		} else{
			System.out.println("\nUm registro de saída com este ID não existe");
		}
	}
}
