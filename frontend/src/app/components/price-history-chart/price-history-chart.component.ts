import { Component, Input, ElementRef, ViewChild, AfterViewInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as echarts from 'echarts';

@Component({
  selector: 'app-price-history-chart',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="position: relative; width: 100%; height: 320px;">
      <div #chartContainer style="width: 100%; height: 100%;"></div>
    </div>
  `
})
export class PriceHistoryChartComponent implements AfterViewInit, OnChanges {
  @Input() snapshots: any[] = [];
  @Input() targetPrice: number = 0;
  @ViewChild('chartContainer') chartContainer!: ElementRef;

  private chartInstance: echarts.ECharts | null = null;

  ngAfterViewInit(): void {
    this.initChart();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['snapshots'] && this.chartInstance) {
      this.updateChart();
    }
  }

  private initChart() {
    if (!this.chartContainer) return;
    this.chartInstance = echarts.init(this.chartContainer.nativeElement, 'dark');
    this.updateChart();

    window.addEventListener('resize', () => {
      this.chartInstance?.resize();
    });
  }

  private updateChart() {
    if (!this.chartInstance) return;

    const data = (this.snapshots && this.snapshots.length > 0) ? this.snapshots : [
      { ts: '2026-09-01T00:00:00Z', sellingPrice: 699, mrp: 999 },
      { ts: '2026-09-05T00:00:00Z', sellingPrice: 649, mrp: 999 },
      { ts: '2026-09-10T00:00:00Z', sellingPrice: 599, mrp: 999 },
      { ts: '2026-09-15T00:00:00Z', sellingPrice: 549, mrp: 999 },
      { ts: '2026-09-20T00:00:00Z', sellingPrice: 399, mrp: 999 }
    ];

    const dates = data.map((d: any) => new Date(d.ts).toLocaleDateString('en-IN', { month: 'short', day: 'numeric' }));
    const prices = data.map((d: any) => d.sellingPrice || d.price || 0);

    const minPrice = Math.min(...prices);

    const option: echarts.EChartsOption = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#0f172a',
        borderColor: '#00F2FE',
        textStyle: { color: '#FFF' },
        formatter: (params: any) => {
          const p = params[0];
          return `<b>${p.name}</b><br/>Selling Price: <span style="color:#00F2FE">₹${p.value}</span>`;
        }
      },
      grid: {
        top: 30,
        left: 50,
        right: 30,
        bottom: 40
      },
      xAxis: {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } },
        axisLabel: { color: '#94A3B8' }
      },
      yAxis: {
        type: 'value',
        axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
        axisLabel: {
          color: '#94A3B8',
          formatter: (v: number) => `₹${v}`
        }
      },
      series: [
        {
          name: 'Price',
          type: 'line',
          smooth: true,
          symbolSize: 8,
          itemStyle: { color: '#00F2FE' },
          lineStyle: { width: 3, color: '#00F2FE' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(0, 242, 254, 0.35)' },
              { offset: 1, color: 'rgba(0, 242, 254, 0.0)' }
            ])
          },
          markPoint: {
            data: [
              { type: 'min', name: 'Lowest Price', itemStyle: { color: '#10B981' } }
            ]
          },
          markLine: this.targetPrice > 0 ? {
            silent: true,
            lineStyle: { type: 'dashed', color: '#FF007A', width: 2 },
            data: [{ yAxis: this.targetPrice, name: 'Target Threshold' }]
          } : undefined,
          data: prices
        }
      ]
    };

    this.chartInstance.setOption(option);
  }
}
