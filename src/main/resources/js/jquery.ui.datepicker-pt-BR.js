/* Brazilian initialisation for the jQuery UI date picker plugin. */
/* Written by Leonildo Costa Silva (leocsilva@gmail.com). */
jQuery(function($){
		$.datepicker.regional['pt'] = {
	            closeText: 'Fechar',
	            prevText: '',
	            nextText: '',
	            currentText: 'Hoje',
	            monthNames: ['Janeiro','Fevereiro','Mar�o','Abril','Maio','Junho',
	            'Julho','Agosto','Setembro','Outubro','Novembro','Dezembro'],
	            monthNamesShort: ['Jan','Fev','Mar','Abr','Mai','Jun',
	            'Jul','Ago','Set','Out','Nov','Dez'],
	            dayNames: ['Domingo','Segunda-feira','Ter�a-feira','Quarta-feira','Quinta-feira','Sexta-feira','Sabado'],
	            dayNamesShort: ['Dom','Seg','Ter','Qua','Qui','Sex','Sab'],
	            dayNamesMin: ['Dom','Seg','Ter','Qua','Qui','Sex','Sab'],
	            weekHeader: 'Sm',
	            dateFormat: 'dd/mm/yy',
	            firstDay: 0,
	            isRTL: false,
	            showMonthAfterYear: false,
	            yearSuffix: ''};

        $.datepicker.regional['pt-BR'] = $.datepicker.regional['pt'];
});