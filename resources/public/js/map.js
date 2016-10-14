$(function() {
	// global vars
	var map, gm, oms, iw;

	// data containers
	var markers = [];
	var ocorrencias = [];
	var heatmapData = [];

	// counters
	function set_counters() {
		var total = 0;
		var roubo = 0;
		var furto = 0;
		var droga = 0;
		var homicidio = 0;
		var outros = 0;

		count_total = function() { return total += 1; }
		count_roubo = function() { return roubo += 1; }
		count_furto = function() { return furto += 1; }
		count_droga = function() { return droga += 1; }
		count_homicidio = function() { return homicidio += 1; }
		count_outros = function() { return outros += 1; }

		get_total = function() { return total; }
		get_roubo = function() { return roubo; }
		get_furto = function() { return furto; }
		get_droga = function() { return droga; }
		get_homicidio = function() { return homicidio; }
		get_outros = function() { return total; }

	}

	var notEven = 1;

	// other
	var $html;
	var heatmap = new google.maps.visualization.HeatmapLayer();

	// Sinop
	var myLatLng = {lat: -11.855275, lng: -55.505966}

	var $form = $('#id_ocorrencias');
	var $info = $('#id_info');
	var $dados_ocorrencias = $('#dados-ocorrencias');

	var WEEKDAY = new Array(7);
	WEEKDAY[0] = "Domingo"
	WEEKDAY[1] = "Segunda"
	WEEKDAY[2] = "Terça"
	WEEKDAY[3] = "Quarta"
	WEEKDAY[4] = "Quinta"
	WEEKDAY[5] = "Sexta"
	WEEKDAY[6] = "Sábado"

	function date_to_string(d) {
		return d.getDate() + "/" + (d.getMonth()+1) + "/" + d.getFullYear();
	}

	var styleType = 'basicMarker';
	$('#styleForm').on('change', function(e) {
		e.preventDefault();
		styleType = $('#id_marker_style').val();
		// Reset the markers and heatmapData array, clearing the map
		clearLocations();
	})

	// Initialize the map, and set the global vars gm, iw, and oms
	initialize();

	$form.on('submit', function(e) {
		/* Main engine of the script. Starts on the form submission. */
		e.preventDefault();
		clearLocations();

		// reset the counters and the container:
		set_counters();
		ocorrencias = [];

		$.ajax({
			type: "GET",
			url: '/analise-criminal/geo/dados',
			data: $form.serialize(),
			success: function(json) {
				if (json.errors) {
					var $error = $('<ul class="errorlist"></ul>');
					for (var key in json.errors) {
						$error.append('<li>' + key + ': ' + json.errors[key][0] + '</li>');
					}
					$info.append($error);
					return;
				}

				$.each(json, function() {
					if (this.numero) {
						this.via += ', ' + this.numero;
					}
					if (this.latitude && this.longitude) {
						var latLng = new google.maps.LatLng(
							parseFloat(this.latitude),
							parseFloat(this.longitude)
						);

						count_natureza(this.natureza);

						if (styleType == 'basicMarker') {
							var address = this.bairro + ', ' + this.via;
							createMarker(latLng, this.id, this.natureza, address);
						} else {
							heatmapData.push(latLng);
						}

						d = new Date(this.data);
						ocorrencias.push({
							natureza: this.natureza, bairro: this.bairro, via: this.via,
							data: date_to_string(d), hora: this.hora,
							weekday: WEEKDAY[d.getDay()]
						});

					} else {
						var notFound = '<br>' + this.id + ': ' + this.bairro + ', ' +
						this.via + ': not found';
						$info.append(notFound);
					}
				});
			},
			fail: function() {
				$info.html('Houve um problema com a solicitação. [AJAX]');
			},
			complete: function() {
				if (ocorrencias.length == 0) {
					return;
				}

				if (styleType == 'heatmap') {
					createHeatmap(heatmapData);
				}

				table(); // html table
				sortableTable(); // allow the table to be sorted

				$html = '<h3>Ocorrências Registradas</h3>';
				if (get_total()) $html += '<span style="padding-right: 30px">Total: ' + get_total() + '</span>';
				if (get_roubo()) $html += '<span style="padding-right: 30px">Roubos: ' + get_roubo() + '</span>';
				if (get_furto()) $html += '<span style="padding-right: 30px">Furtos: ' + get_furto() + '</span>';
				if (get_droga()) $html += '<span style="padding-right: 30px">Entorpecentes: ' + get_droga() + '</span>';
				if (get_homicidio()) $html += '<span style="padding-right: 30px">Homicídios: ' + get_homicidio() + '</span>';
				if (get_outros()) $html += '<span>Outras ocorrências: ' + get_outros() + '</span>';

				$info.append($html);
			}
		}); // END of ajax call
	}); // END of form

	oms.addListener('spiderfy', function(markers) {
		iw.close();
	});

	function createMarker(latlng, id, natureza, address) {
		/* Creates each indiviual marker of the map */
		var html = "<b> id: " + id + ' ' + natureza + "</b> <br />" + address;

		var markerColor, markerText;
		if (/furto/i.test(natureza)) {
			markerColor = "E25A5A";
			markerText = "F"
		} else if (/roubo/i.test(natureza)) {
			markerColor = "fff";
			markerText = "R";
		} else if (/hom/i.test(natureza)){
			markerColor = "000";
			markerText = "H";
		} else if (/drogas/i.test(natureza)){
			markerColor = "b4eeb4";
			markerText = "E";
		} else {
			markerColor = "ddd";
			markerText = "O";
			count_outros();
		}

		var marker = new StyledMarker({
			styleIcon: new StyledIcon(StyledIconTypes.MARKER,{color: markerColor,text: markerText}),
			position:latlng,
			map: map
		});

		google.maps.event.addListener(marker, 'click', function() {
			iw.setContent(html);
			iw.open(map, marker);
		});

		google.maps.event.addListener(marker, 'mouseover', function() {
			marker.setOpacity(0.5);
		});

		google.maps.event.addListener(marker, 'mouseout', function() {
			marker.setOpacity(1);
		});

		markers.push(marker);
		oms.addMarker(marker);

	} // END OF createMarker()

	function count_natureza(natureza) {
		/*
		Checks what natureza is included in a string, and counts how
		many times it has appeared.
		*/
		if (/roubo/i.test(natureza)) {
			count_roubo();
		} else if (/furto/i.test(natureza)) {
			count_furto();
		} else if (/homic[ií]dio/i.test(natureza)) {
			count_homicidio();
		} else if (/drogas/i.test(natureza)) {
			count_droga();
		} else {
			count_outros();
		}
		count_total();
	}

	function createHeatmap(heatmapData) {
		/*
		Takes an array of latlng values, and sets the tone for the map.
		*/
		heatmap = new google.maps.visualization.HeatmapLayer({
			data: heatmapData,
			dissipating: false,
			map: map
		});
		/* If I remember, this options weren't working the way I wanted;
		** Research and refactor. */
		heatmap.set('radius', 1);
		heatmap.set('scaleRadius', false);
	}

	function table() {
		/* Creates the table based on the data given by the ajax call
			 global: ocorrencias */
		var $table = $('<table class="sortable"></table>');
		var $thead = $(
			"<thead>" +
				"<tr>" +
					"<th data-sort='name'>Natureza</th>" +
					"<th data-sort='name'>Bairro</th>" +
					"<th data-sort='name'>Via</th>" +
					"<th data-sort='date'>Data</th>" +
					"<th data-sort='weekday'>Dia da semana</th>" +
					"<th data-sort='duration'>Hora</th>" +
				"</tr>" +
			"</thead>"
		);

		$table.append($thead);
		var $tableBody = $('<tbody></tbody>');

		for (var i = 0; i < ocorrencias.length; i++) {
			var ocorrencia = ocorrencias[i];

			if (notEven % 2 == 0) {
				var $row = $('<tr class="even"></tr>');
			} else {
				var $row = $('<tr></tr>');
			}

			$row.append( $('<td></td>').text(ocorrencia.natureza) );
			$row.append( $('<td></td>').text(ocorrencia.bairro) );
			$row.append( $('<td></td>').text(ocorrencia.via) );
			$row.append( $('<td></td>').text(ocorrencia.data) );
			$row.append( $('<td></td>').text(ocorrencia.weekday) );
			$row.append( $('<td></td>').text(ocorrencia.hora) );
			$tableBody.append( $row );

			notEven++
		}
		$table.append($tableBody);
		$dados_ocorrencias.append($table);
	} // table()

	function sortableTable() {
		/* Allows the tables to be sorted */
		var compare = {
			name: function(a, b) {
				a = a.replace(/^(rua)|(avenida)|(jardim)\s/i, '');
				b = b.replace(/^(rua)|(avenida)|(jardim)\s/i, '');

				if (a < b) return -1;
				else return a > b ? 1 : 0;
			},
			duration: function(a, b) {
				if (a == 'null') {
					return 1;
				} else if (b == 'null') {
					return -1;
				}
				a = a.split(':');
				b = b.split(':');

				a = Number(a[0]) * 60 + Number(a[1]);
				b = Number(b[0]) * 60 + Number(b[1]);

				return a - b;
			},
			date: function(a, b) {
				a = returnDate(a);
				b = returnDate(b);
				function returnDate(dateString) {
					var day = dateString.split('/')[0];
					var month = dateString.split('/')[1];
					var year = dateString.split('/')[2];
					var dateObj = new Date(year, month, day);
					return dateObj.getTime();
				}
				a = new Date(a);
				b = new Date(b);

				return a - b;
			},
			weekday: function(a, b) {
				function returnWeekday(x) {
					switch (x) {
						case 'Segunda': x = 1; break;
						case 'Terça': x = 2; break;
						case 'Quarta': x = 3; break;
						case 'Quinta': x = 4; break;
						case 'Sexta': x = 5; break;
						case 'Sábado': x = 6; break;
						case 'Domingo': x = 7; break;
					}
					return x;
				}
				a = returnWeekday(a);
				b = returnWeekday(b);

				return a - b;
			}
		}; // compare

		$('.sortable').each(function() {

			var $table = $(this);
			var $tbody = $table.find('tbody');
			var $controls = $table.find('th');
			var rows = $tbody.find('tr').toArray();

			$controls.on('click', function() {
				$('.even').removeClass('even');

				var $header = $(this);
				var order = $header.data('sort');
				var column;

				if ($header.is('.ascending') || $header.is('.descending')) {
					$header.toggleClass('ascending descending');
					$tbody.append(rows.reverse());

					$('.sortable tr:odd').addClass('even');
				} else {
					$header.addClass('ascending');
					$header.siblings().removeClass('ascending descending');
					if (compare.hasOwnProperty(order)) {
						column = $controls.index(this);

						rows.sort(function(a, b) {
							a = $(a).find('td').eq(column).text();
							b = $(b).find('td').eq(column).text();
							return compare[order](a, b);
						});
						$tbody.append(rows);

						$('.sortable tr:odd').addClass('even');
					}
				}
			});
		});
	} // sortableTable()

	function initialize() {
		/* Initializes the map, and sets the global vars gm, iw, and oms. */
		map = new google.maps.Map(document.getElementById('map'), {
			zoom: 15,
			center: myLatLng,
			mapTypeId: google.maps.MapTypeId.ROADMAP
		});

		gm = google.maps;
		iw = new gm.InfoWindow();
		oms = new OverlappingMarkerSpiderfier(map);
	}


	function clearLocations() {
		/*  Resets the markers and heatmapData array, clearing the map */
		iw.close();
		for (var i = 0; i < markers.length; i++) {
			markers[i].setMap(null);
		}
		markers.length = 0;
		heatmapData.length = 0;
		heatmap.setMap(null);
		document.getElementById('errors').innerHTML = '';
		$info.html('');
		$dados_ocorrencias.html('');
	}
});
